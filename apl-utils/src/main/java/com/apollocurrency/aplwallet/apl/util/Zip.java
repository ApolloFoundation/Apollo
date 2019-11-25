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
     * Compress all filtered files in directory into ZIP file, change file timestamp to be predefined.Return computed CRC/hash for created ZIP as byte array.
     *
     * @param zipFile path to zip file
     * @param inputFolder folder to zip
     * @param filesTimeFromEpoch NULL (default will be assigned) or time in ms from Epoch for all file times
     * @param filenameFilter NULL (CSV will be by default) or file filter instance
     * @param recursive with subdirs
     * @return ChunkedFileOps containing hash and partial hashes if success or null, when exception occurred or files for compress were not found
     */
    ChunkedFileOps compressAndHash(String zipFile, String inputFolder, Long filesTimeFromEpoch, FilenameFilter filenameFilter, boolean  recursive);


    /**
     * Compress all filtered files in directory into ZIP file, change file timestamp to be predefined.Return computed CRC/hash for created ZIP as byte array.
     *
     * @param zipFile path to zip file
     * @param inputFolder folder to zip
     * @param filesTimeFromEpoch NULL (default will be assigned) or time in ms from Epoch for all file times
     * @param filenameFilter NULL (CSV will be by default) or file filter instance
     * @param recursive with subfirs
     * @return true, when archive was successfully created and false if there are no CSV file inside specified folder or exception occurred during compression
     */
     boolean compress(String zipFile, String inputFolder, Long filesTimeFromEpoch, FilenameFilter filenameFilter, boolean  recursive);
}
