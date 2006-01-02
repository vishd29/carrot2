
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
package com.chilang.util;

public class MathUtils {
    private MathUtils(){}

    /**
     * Calculate logarithm of base for an argument
     * @param base logarithm base
     * @param arg argument
     * @return
     */
    public static double log(double base, double arg) {
        return Math.log(arg) / Math.log(base);
    }
    
    public static double log10(double arg) {
        return Math.log(arg) / Math.log(10);
    }
}
