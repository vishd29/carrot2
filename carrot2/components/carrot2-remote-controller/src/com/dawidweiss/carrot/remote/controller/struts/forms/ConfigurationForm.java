
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
package com.dawidweiss.carrot.remote.controller.struts.forms;


import org.apache.struts.action.*;
import java.util.Locale;
import javax.servlet.http.*;


/**
 * User-preferences form.
 */
public class ConfigurationForm
    extends ActionForm
{
    private Locale locale;
    protected boolean queryDetailsVisible;

    public ConfigurationForm()
    {
        locale = Locale.getDefault();
    }

    public boolean isQueryDetailsVisible()
    {
        return queryDetailsVisible;
    }


    public void setQueryDetailsVisible(boolean value)
    {
        this.queryDetailsVisible = value;
    }


    public void setLocale(String localeString)
    {
        this.locale = parseLocale(localeString);
    }


    public String getLocale()
    {
        if ((locale == null) || (locale.getLanguage() == null))
        {
            return "";
        }
        else
        {
            return locale.getLanguage();
        }
    }


    public void reset(ActionMapping mapping, javax.servlet.http.HttpServletRequest request)
    {
        locale = request.getLocale();
    }


    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request)
    {
        return null;
    }


    private Locale parseLocale(String s)
    {
        String [] tokens = { "", "", "" };
        int position;
        int currentToken = 0;

        while ((currentToken < 2) && ((position = s.indexOf('_')) != -1))
        {
            tokens[currentToken++] = s.substring(0, position);
            s = s.substring(position + 1);
        }

        tokens[currentToken] = s;

        return new Locale(tokens[0], tokens[1], tokens[2]);
    }
}
