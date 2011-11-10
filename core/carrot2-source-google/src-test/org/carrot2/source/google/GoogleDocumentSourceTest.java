
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2011, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.source.google;

import java.util.List;

import org.carrot2.core.Document;
import org.carrot2.core.test.MultipageDocumentSourceTestBase;
import org.carrot2.source.MultipageSearchEngineMetadata;
import org.carrot2.util.tests.UsesExternalServices;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeaks;

/**
 * Test cases for {@link GoogleDocumentSource}.
 */
@UsesExternalServices
@ThreadLeaks(linger = 2000)
public class GoogleDocumentSourceTest extends
    MultipageDocumentSourceTestBase<GoogleDocumentSource>
{
    @Override
    public Class<GoogleDocumentSource> getComponentClass()
    {
        return GoogleDocumentSource.class;
    }

    @Override
    protected MultipageSearchEngineMetadata getSearchEngineMetadata()
    {
        return GoogleDocumentSource.metadata;
    }

    @Override
    protected int getLargeQuerySize()
    {
        return 32;
    }

    @Override
    protected int getSmallQuerySize()
    {
        return 16;
    }

    @Override
    protected boolean hasUtfResults()
    {
        return true;
    }

    @Test
    public void testHighlightsRemoved()
    {
        runQuery("test", getSmallQuerySize());
        final List<Document> documents = getDocuments();
        for (Document document : documents)
        {
            final String snippet = document.getField(Document.SUMMARY);
            assertThat(snippet).doesNotMatch(".*</?b>.*");
        }
    }

    @Test
    public void testNoUrlEncodingInUrls()
    {
        runQuery("help", getSmallQuerySize());

        final List<Document> documents = getDocuments();
        for (Document document : documents)
        {
            final String url = document.getField(Document.CONTENT_URL);
            assertThat(url).doesNotMatch(".*%3[Ff].*");
        }
    }
}
