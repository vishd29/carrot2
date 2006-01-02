
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2006, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.cs.put.poznan.pl/dweiss/carrot2.LICENSE
 */
package com.dawidweiss.carrot.remote.controller;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.exolab.castor.xml.Unmarshaller;

import bsh.util.BeanShellBSFEngine;

import com.dawidweiss.carrot.remote.controller.cache.AbstractFilesystemCachedQueriesContainer;
import com.dawidweiss.carrot.remote.controller.cache.Cache;
import com.dawidweiss.carrot.remote.controller.cache.CachedQueriesContainer;
import com.dawidweiss.carrot.remote.controller.components.ComponentsLoader;
import com.dawidweiss.carrot.remote.controller.guard.QueryGuard;
import com.dawidweiss.carrot.remote.controller.guard.QueryGuardsSet;
import com.dawidweiss.carrot.remote.controller.process.ProcessDefinition;
import com.dawidweiss.carrot.remote.controller.process.ProcessingChainLoader;
import com.dawidweiss.carrot.remote.controller.util.Loader;
import com.dawidweiss.carrot.util.Log4jStarter;


/**
 * Application initialization. This servlet reads all configuration files and prepares environment
 * for Struts actions by placing references to processor and other components in application
 * context's attributes.
 */
public class Carrot2InitServlet
    extends HttpServlet
{
    /** Logger instance */
    private static final Logger log = Logger.getLogger(Carrot2InitServlet.class);

    /**
     * If this argument is present in application context, the initialization failed for some
     * reason. The value held by this argument should be a Throwable denoting the cause of
     * initialization failure.
     */
    public static final String CARROT_INIT_ERROR_KEY = "CARROT_INIT_ERROR";
    public static final String CARROT_PROCESSOR_KEY = "CARROT_PROCESSOR";
    public static final String CARROT_PROCESSINGCHAINS_LOADER = "CARROT_PROCESSINGCHAINS_LOADER";

    /** Main configuration file. */
    private File config;

    /** Allows dynamic configuration reloading. Disable by default. */
    private boolean allowReloading = false;

    /** Extra configuration properties */    
    private HashMap properties = new HashMap(3);


    /**
     * Initialize Carrot2 environment.
     *
     * @param servletConfig Servlet configuration passed from servlet container.
     */
    public void init(ServletConfig servletConfig)
        throws ServletException
    {
        super.init(servletConfig);

        // Install beanshell support.
        org.apache.bsf.BSFManager.registerScriptingEngine(
            "beanshell", BeanShellBSFEngine.class.getName(), new String [] { "bsh" }
        );

        String configFileName = servletConfig.getInitParameter("config");
        initialize(
            servletConfig.getServletContext(),
            new File(servletConfig.getServletContext().getRealPath(configFileName))
        );
    }


    /**
     * GET method may be used to reconfigure Carrot2 processing environment. It basically flushes
     * all context references and re-reads configuration files. This option must be allowed in the
     * primary configuration XML.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        if (allowReloading)
        {
            // reload configuration file and recreate environment.
            try
            {
                log.info("Configuration reload requested [" + new Date() + "]");
                initialize(getServletContext(), config);
                getServletContext().getRequestDispatcher("/").forward(request, response);
            }
            catch (Exception e)
            {
                log.error("Error when reloading configuration.", e);
                request.setAttribute("exception", e);

                try
                {
                    getServletContext().getRequestDispatcher("/error.jsp").forward(
                        request, response
                    );
                }
                catch (IOException x)
                {
                    log.fatal("Cannot redirect to error.jsp", x);
                    throw new ServletException("No errors page.", x);
                }
            }
        }
        else
        {
            // not allowed. Send HTTP error.
            try
            {
                response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Carrot2 configuration set to non-reloading mode."
                );
            }
            catch (IOException e)
            {
                // simply ignore requests when reloading disabled.
                log.warn("GET request when reloading disabled from " + request.getRemoteHost());
            }
        }
    }


    /* -------------------------------------------------------------------- protected methods */

    /**
     * Loads the XML configuration file and initializes environment.
     */
    private void initialize(ServletContext context, File configurationFile)
    {
        try
        {
            if ((configurationFile == null) || !configurationFile.canRead())
            {
                // cannot read configuration file. Set up context setup error.
                throw new RuntimeException("errors.initialization.missing-config");
            }
            else
            {
                config = configurationFile;

                // load the XML configuration resource.
                SAXReader configBuilder = new SAXReader(false);
                Element root = configBuilder.read(configurationFile).getRootElement();

                if ((root.attribute("reloadable") != null)
                        && "true".equalsIgnoreCase(root.attribute("reloadable").getValue()))
                {
                    this.allowReloading = true;
                }
                else
                {
                    this.allowReloading = false;
                }

                // initialize log4j.
                Log4jStarter.getLog4jStarter().initializeLog4j(getServletConfig());

                // load the default set of components
                ComponentsLoader componentLoader = new ComponentsLoader();
                List defaultComponents = root.elements("components");
                for (Iterator i = defaultComponents.iterator(); i.hasNext();)
                {
                    Element e = (Element) i.next();
                    componentLoader.addComponentsFromDirectory(
                        new File(context.getRealPath(e.attribute("contextDir").getValue()))
                    );
                }

                // load the default set of processing chains and associate them
                // with components loaded.
                ProcessingChainLoader processLoader = new ProcessingChainLoader(componentLoader);
                List defaultProcesses = root.elements("processes");

                for (Iterator i = defaultProcesses.iterator(); i.hasNext();)
                {
                    Element e = (Element) i.next();
                    processLoader.addProcessesFromDirectory(
                        new File(context.getRealPath(e.attribute("contextDir").getValue()))
                    );
                }
                
                // Set the default process to use.
                List defaultProcess = root.elements("default-process");
                if (defaultProcess.size() > 1)
                {
                    throw new RuntimeException("errors.initialization.too-many-default-processes");
                }
                else
                {
                    if (defaultProcess.size() != 0)
                    {
                        String processId = ((Element) defaultProcess.get(0)).attributeValue("id");
                        log.debug("Setting default process to: " + processId);
                        ProcessDefinition process = processLoader.findProcessDefinition(processId);
                        if (process == null)
                        {
                            throw new RuntimeException("errors.initialization.missing-default-context");
                        }
                        processLoader.setDefaultProcess(process);
                    }
                }

                // Set properties
                List propertiesElementsList = root.elements("properties");
                if (propertiesElementsList.size() > 1)
                {
                    throw new RuntimeException("errors.initialization.too-many-properties");
                }
                else
                {
                    if (propertiesElementsList.size() != 0)
                    {
                        propertiesElementsList = ((Element) propertiesElementsList.get(0)).elements();
                        
                        for (Iterator i = propertiesElementsList.iterator();i.hasNext();)
                        { 
                            Element propertyElement = (Element) i.next();
                            if (!"property".equals(propertyElement.getName()))
                            {
                                throw new RuntimeException("errors.initialization.config-structure-error");
                            }
                           
                            String propName = propertyElement.attributeValue("name");
                            String value = propertyElement.attributeValue("value");
                            if (propName == null || value == null)
                            {
                                throw new RuntimeException("errors.initialization.config-structure-error");
                            }
                            
                            this.properties.put(propName, value);
                        }
                    }
                }

                //
                // Instantiate caches and QueryProcessor
                //
                Cache cache;

                Element cacheConfig = root.element("cache");

                if ((cacheConfig != null) && (cacheConfig.attribute("config") != null))
                {
                    File cacheConfigFile = new File(
                            context.getRealPath(cacheConfig.attribute("config").getValue())
                        );

                    if (!cacheConfigFile.exists() || !cacheConfigFile.canRead())
                    {
                        log.error(
                            "Cache configuration file cannot be read: "
                            + cacheConfigFile.getAbsolutePath()
                        );
                        throw new RuntimeException("errors.initialization.cache-config-missing");
                    }

                    // parse the cache specification XML.
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.setValidating(false);

                    DocumentBuilder builder = factory.newDocumentBuilder();
                    org.w3c.dom.Document document = builder.parse(cacheConfigFile);

                    org.w3c.dom.NodeList nlist = document.getElementsByTagName("container");

                    cache = new Cache();

                    for (int i = 0; i < nlist.getLength(); i++)
                    {
                        org.w3c.dom.Node container = nlist.item(i);
                        String implementation = container.getAttributes()
                                                         .getNamedItem("implementation")
                                                         .getNodeValue();

                        Class clazz = this.getClass().getClassLoader().loadClass(implementation);
                        CachedQueriesContainer cqc = (CachedQueriesContainer) Unmarshaller
                            .unmarshal(clazz, container);
                        cache.addContainer(cqc);

                        if (cqc instanceof AbstractFilesystemCachedQueriesContainer)
                        {
                            ((AbstractFilesystemCachedQueriesContainer) cqc).setServletBase(
                                this.getServletContext().getRealPath("/")
                            );
                        }
                    }
                }
                else
                {
                    cache = new Cache();
                }

                cache.configure();

                //
                // Instantiate QueryGuard(s)
                //
                QueryGuard guard = null;

                Element guardDirElement = root.element("guards");

                if (
                    (guardDirElement != null)
                        && (guardDirElement.attribute("contextDir") != null)
                )
                {
                    File guardsDir = new File(
                            context.getRealPath(
                                guardDirElement.attribute("contextDir").getValue()
                            )
                        );

                    if (!guardsDir.isDirectory())
                    {
                        log.error(
                            "Guards directory cannot be read: " + guardsDir.getAbsolutePath()
                        );
                        throw new RuntimeException("errors.initialization.guards-dir-unavailable");
                    }

                    File [] guards = guardsDir.listFiles(
                            new FileFilter()
                            {
                                public boolean accept(File f)
                                {
                                    return f.isFile() && f.getName().endsWith(".xml");
                                }
                            }
                        );

                    if (guards.length > 0)
                    {
                        Loader loader = Loader.loadLoader(
                                QueryGuard.class.getResourceAsStream("loader-config.xml")
                            );

                        guard = new QueryGuardsSet();

                        for (int i = 0; i < guards.length; i++)
                        {
                            log.debug("Adding query guard: " + guards[i].getName());

                            try
                            {
                                QueryGuard g = (QueryGuard) loader.load(
                                        new FileInputStream(guards[i])
                                    );
                                ((QueryGuardsSet) guard).addGuard(g);
                            }
                            catch (Exception e)
                            {
                                log.error(
                                    "Cannot load guard definition from file: "
                                    + guards[i].getAbsolutePath(), e
                                );
                                throw new RuntimeException(
                                    "Cannot load guard definition from file: "
                                    + guards[i].getAbsolutePath()
                                );
                            }
                        }
                    }
                }

                // Instantiate QueryProcessor
                QueryProcessor processor = new QueryProcessor(cache, guard);
                context.setAttribute(Carrot2InitServlet.CARROT_PROCESSOR_KEY, processor);
                processor.setFullDebugInfo(
                    Boolean.valueOf(root.attributeValue("fullDebugReport", "false"))
                           .booleanValue()
                );
                
                //
                // Check properties. 
                //
                
                if (this.properties.containsKey("request-history"))
                {
                    int v = Integer.valueOf((String) properties.get("request-history")).intValue();
                    if (v <= 0)
                        throw new IllegalArgumentException("Request history must be greater than zero.");
                    processor.setRequestHistory(new RequestHistory(v));
                }


                //
                // Add processing chains to the application context.
                //
                context.setAttribute(
                    Carrot2InitServlet.CARROT_PROCESSINGCHAINS_LOADER, processLoader
                );
            }

            context.removeAttribute(CARROT_INIT_ERROR_KEY);
        }
        catch (Throwable e)
        {
            log.fatal("Exception raised when initializing application.", e);
            context.setAttribute(CARROT_INIT_ERROR_KEY, e);
        }
    }
}
