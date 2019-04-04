package com.apollocurrency.aplwallet.apl.testutil;

import java.io.File;

public class FileLoader {

    public File getFile(String fileName){
        ClassLoader classLoader = getClass().getClassLoader();
        return  new File(classLoader.getResource(fileName).getFile());
    }

}
