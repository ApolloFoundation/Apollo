package com.apollocurrency.aplwallet.apl.util;

import java.io.File;

/**
 * Resource find helper.
 *
 * @author alukin@gmail.com
 */
public class ResourcePaths {

    public static String[] searchPaths = {"", "../", "../../"};

    /**
     * Finds file. Search order: current dir, one level upper dir, 2 level upper
     * dir, see searchPaths 
     *
     * @param path
     * @return absolute path if found, empty string if not
     */
    public static String find(String path) {
        String res = path;
        boolean found = false;
        for (String prefix : searchPaths) {
            File f = new File(prefix + path);
            if (f.exists()) {
                found = true;
                res = f.getAbsolutePath();
                break;
            }
        }
        if (!found) {
            res = "";
        }
        return res;
    }
}
