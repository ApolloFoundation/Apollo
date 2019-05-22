/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

import org.h2.store.fs.FilePath;
import org.slf4j.Logger;

/**
 * Utility for file related operations
 *
 * @author yuriy.larin
 */
public class CsvFileUtils {
    private static final Logger log = getLogger(CsvFileUtils.class);

    /**
     * Create an output stream to write into the file.
     * This method is similar to Java 7
     * <code>java.nio.file.Path.newOutputStream</code>.
     *
     * @param fileName the file name
     * @param append if true, the file will grow, if false, the file will be
     *            truncated first
     * @return the output stream
     */
    public static OutputStream newOutputStream(Path dataExportPath, String fileName, boolean append)
            throws IOException {
        Objects.requireNonNull(dataExportPath, "dataExportPath is NULL");
        Objects.requireNonNull(fileName, "fileName is NULL");
        FilePath filePath = FilePath.get(dataExportPath.resolve(fileName).toString());
        log.debug("new output file by path = '{}'", filePath.toRealPath());
        return filePath.newOutputStream(append);
    }

    /**
     * Create an input stream to read from the file.
     * This method is similar to Java 7
     * <code>java.nio.file.Path.newInputStream</code>.
     *
     * @param dataExportPath folder with CSV files to import
     * @param fileName the file name
     * @return the input stream
     */
    public static InputStream newInputStream(Path dataExportPath, String fileName)
            throws IOException {
        Objects.requireNonNull(dataExportPath, "dataExportPath is NULL");
        Objects.requireNonNull(fileName, "fileName is NULL");
        FilePath filePath = FilePath.get(dataExportPath.resolve(fileName).toString());
        log.debug("new input file by path = '{}'", filePath.toRealPath());
        return filePath.newInputStream();
    }

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
