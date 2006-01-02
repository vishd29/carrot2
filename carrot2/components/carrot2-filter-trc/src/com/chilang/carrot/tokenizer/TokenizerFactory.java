
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
package com.chilang.carrot.tokenizer;

public class TokenizerFactory {

    public static final int HTML = 1;

    private TokenizerFactory() {};

    public static ITokenizer getTokenizer() {
        return new HTMLAwareTokenizer(new CommonEntityResolver());
    }

    public static ITokenizer getTokenizer(int type) {
        switch(type) {
            case HTML :
                return new HTMLAwareTokenizer(new CommonEntityResolver());
            default :
                return new DefaultTokenizer();
        }
    }
}
