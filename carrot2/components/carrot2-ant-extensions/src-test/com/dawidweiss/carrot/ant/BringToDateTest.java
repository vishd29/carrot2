
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
package com.dawidweiss.carrot.ant;

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the corresponding ANT task.
 */
public class BringToDateTest extends BuildFileTest {

    public BringToDateTest(String s) {
        super(s);
    }

    public void setUp() {
        configureProject("ant-tests/bringToDate.xml");
    }

    public void testBringToDate() {
        try {
            executeTarget("test");
        } finally {
            System.out.println(super.getLog());
        }
    }

    public void testProfileDependency() {
        executeTarget("test2");
    }

    public void testCircularDependency() {
    	super.expectBuildException("test3", "Circular dependency");
    }

    public void testCheckFilesInSubprojectsUpToDate() {
    	super.executeTarget("checkFilesInSubprojectsUpToDate");
    }
}
