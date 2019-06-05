package com.apollocurrency.aplwallet.apl.util;

import java.io.FilenameFilter;

public interface Zip {

    /**
     * Extract zip file into directory
     * @param zipFile zip file
     * @param outputFolder output directory
     * @return true if success
     */
    boolean extract(String zipFile, String outputFolder);

    /**
     * Compress all filtered files in directory into ZIP file, change file timestamp to be predefined.
     *
     * @param zipFile result zip file with path
     * @param inputFolder folder to zip
     * @param filesTimeFromEpoch NULL (default will be assigned) or time in ms from Epoch for all file times
     * @param filenameFilter NULL (CSV will be by default) or file filter instance
     * @return true if success
     * @throws RuntimeException if there are no CSF file inside specified folder
     */
    boolean compress(String zipFile, String inputFolder, Long filesTimeFromEpoch, FilenameFilter filenameFilter);
}
