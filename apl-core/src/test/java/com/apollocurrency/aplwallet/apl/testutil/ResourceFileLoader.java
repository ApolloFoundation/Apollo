package com.apollocurrency.aplwallet.apl.testutil;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

import static org.slf4j.LoggerFactory.getLogger;

public class ResourceFileLoader {
    private static final Logger log = getLogger(ResourceFileLoader.class);

    public File getFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(fileName).getPath());
    }

    /**
     * Fails if there is no "logback-test.xml" file in module with tests
     *
     * @return unit test resource folder
     */
    public Path getResourcePath() {
        ClassLoader classLoader = getClass().getClassLoader();
        // "logback-test.xml" should be present in resource folder, otherwise it fails
        File file = new File(classLoader.getResource("logback-test.xml").getFile()); // usually we have it there
//        File file = new File("/media/ylarin/PHOTO/java_projects/Apollo/tmp/public_key.csv"); // usually we have it there
        log.trace(file.getAbsolutePath());
        return new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator))).toPath();
    }

}
