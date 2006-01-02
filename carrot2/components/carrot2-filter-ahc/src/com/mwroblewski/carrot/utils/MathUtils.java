
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
package com.mwroblewski.carrot.utils;


/**
 * @author Micha� Wr�blewski
 */
public class MathUtils
{
    public static float log(float base, float value)
    {
        // calculating ln (x)
        float result = (float) Math.log(value);

        // converting to log a (x)
        result /= Math.log(base);

        return value;
    }


    public static float round(float value, float precision)
    {
        int tmp = Math.round(value / precision);

        return tmp * precision;
    }
}
