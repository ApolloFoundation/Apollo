package com.apollocurrency.aplwallet.apl.testutil;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.slf4j.Logger;

public class ResourceFileLoader {
    private static final Logger log = getLogger(ResourceFileLoader.class);

    public File getFile(String fileName){
        ClassLoader classLoader = getClass().getClassLoader();
        return  new File(classLoader.getResource(fileName).getPath());
    }

    /**
     * Fails if there is no "logback-test.xml" file in module with tests
     *
     * @return unit test resource folder
     */
    public Path getResourcePath(){
        ClassLoader classLoader = getClass().getClassLoader();
        // "logback-test.xml" should be present in resource folder, otherwise it fails
        try {
            URI uri = Objects.requireNonNull(classLoader.getResource("logback-test.xml")).toURI();// usually we have it there
            return Paths.get(uri).getParent().toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}
