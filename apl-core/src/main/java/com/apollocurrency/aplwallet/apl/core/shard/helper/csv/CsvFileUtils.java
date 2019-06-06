/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import java.io.Reader;
import java.io.Writer;

/**
 * Utility for file related operations
 *
 * @author yuriy.larin
 */
public class CsvFileUtils {

    /**
     * Close a reader without throwing an exception.
     *
     * @param reader the reader or null
     */
    public static void closeSilently(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Close a writer without throwing an exception.
     *
     * @param writer the writer or null
     */
    public static void closeSilently(Writer writer) {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }


}
