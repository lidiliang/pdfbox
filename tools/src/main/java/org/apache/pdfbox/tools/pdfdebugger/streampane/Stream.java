/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.tools.pdfdebugger.streampane;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * @author Khyrul Bashar
 *
 * A class that provides the COSStream in different version and related informations.
 */
class Stream
{
    public static final String UNFILTERED = "unfiltered";
    public static final String FILTERED = "Filtered";
    public static final String IMAGE = "Image";

    private final COSStream stream;
    private final boolean isImage;
    private final Map<String, List<String>> filters;

    /**
     * Constructor.
     * @param cosStream COSStream instance.
     */
    Stream(COSStream cosStream)
    {
        this.stream = cosStream;
        this.isImage = isImageStream(cosStream);

        filters = createFilterList(cosStream);
    }

    /**
     * Return if this is stream is an Image XObject.
     * @return true if this an image and false otherwise.
     */
    public boolean isImage()
    {
        return isImage;
    }

    /**
     * Return the available filter list. Only "Unfiltered" is returned if there is no filter and in case of
     * XObject image type stream "Image" is also included in the list.
     * @return An array of String.
     */
    public String[] getFilterList ()
    {
        Set<String> set = filters.keySet();
        String[] filterArray = new String[set.size()];
        return set.toArray(filterArray);
    }

    /**
     * Returns a InputStream of a partially filtered stream.
     *
     * @param key is an instance of String which tells which version of stream should be returned.
     * @return an InputStream.
     */
    public InputStream getStream(String key)
    {
        try
        {
            if (UNFILTERED.equals(key))
            {
                return stream.getUnfilteredStream();
            }
            else if (FILTERED.equals(key))
            {
                return stream.getFilteredStream();
            }
            else
            {
                return new PDStream(stream).getPartiallyFilteredStream(filters.get(key));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Provide the image for stream. The stream must be image XObject.
     * @param resources PDResources for the XObject.
     * @return A BufferedImage.
     */
    public BufferedImage getImage(PDResources resources)
    {
        try
        {
            PDImageXObject imageXObject = new PDImageXObject(new PDStream(stream), resources);
            return imageXObject.getImage();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, List<String>> createFilterList(COSStream stream)
    {
        HashMap<String, List<String>> filterList = new HashMap<String, List<String>>();

        filterList.put(UNFILTERED, null);
        PDStream pdStream = new PDStream(stream);

        if (pdStream.getFilters() != null)
        {
            int filtersSize = pdStream.getFilters().size();

            for (int i = filtersSize - 1; i >= 1; i--)
            {
                filterList.put(getPartialStreamCommand(i), getStopFilterList(i));
            }
            filterList.put(FILTERED, null);
        }
        if (isImage)
        {
            filterList.put(IMAGE, null);
        }
        return filterList;
    }

    private String getPartialStreamCommand(final int indexOfStopFilter)
    {
        List<COSName> avaiablrFilters = new PDStream(stream).getFilters();

        StringBuilder nameListBuilder = new StringBuilder();
        for (int i = indexOfStopFilter; i < avaiablrFilters.size(); i++)
        {
            nameListBuilder.append(avaiablrFilters.get(i).getName()).append(" & ");
        }
        nameListBuilder.delete(nameListBuilder.lastIndexOf("&"), nameListBuilder.length());

        return "Keep " + nameListBuilder.toString() + "...";
    }

    private List<String> getStopFilterList(final int stopFilterIndex)
    {
        List<COSName> avaiablrFilters = new PDStream(stream).getFilters();

        final List<String> stopFilters = new ArrayList<String>(1);
        stopFilters.add(avaiablrFilters.get(stopFilterIndex).getName());

        return stopFilters;
    }

    private boolean isImageStream(COSDictionary dic)
    {
        return dic.containsKey(COSName.SUBTYPE) && dic.getCOSName(COSName.SUBTYPE).equals(COSName.IMAGE);
    }
}
