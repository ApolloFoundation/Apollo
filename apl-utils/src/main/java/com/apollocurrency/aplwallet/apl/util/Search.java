/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

 /*
 * Copyright © 2019-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public final class Search {

    private static final Logger LOG = getLogger(Search.class);

    private static final Analyzer analyzer = new StandardAnalyzer();

    private Search() {
    }

    public static String[] parseTags(String tags, int minTagLength, int maxTagLength, int maxTagCount) {
        if (tags == null || tags.trim().length() == 0) {
            return Convert.EMPTY_STRING;
        }
        List<String> list = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(null, tags)) {
            CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);
            String tag;
            stream.reset();
            while (stream.incrementToken() && list.size() < maxTagCount
                    && (tag = attribute.toString()).length() <= maxTagLength && tag.length() >= minTagLength) {
                if (!list.contains(tag)) {
                    list.add(tag);
                }
            }
            stream.end();
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return list.toArray(new String[0]);
    }

    public static String detectMimeType(byte[] data, String filename) {
        //try to guess from file magic numers, it is more reliable
        String mimeType = detectMimeType(data);
        if (mimeType == null) {
            //guess from file name
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            mimeType = fileNameMap.getContentTypeFor("file://" + filename);
        }
        return mimeType;
    }

    public static String detectMimeType(byte[] data) {
        String mimeType = null;
        InputStream is = new BufferedInputStream(new ByteArrayInputStream(data));
        try {
            mimeType = URLConnection.guessContentTypeFromStream(is);
        } catch (IOException ex) {
            //do nothing, stream is in memory
        }
        return mimeType;
    }

    protected static boolean parseIps(List<String> ips) {
        Pattern IP_PATTERN = Pattern.compile(
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return ips.stream().anyMatch(ip -> !IP_PATTERN.matcher(ip).matches());
    }

}
