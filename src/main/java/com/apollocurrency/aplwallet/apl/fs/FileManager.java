/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.fs;

public class FileManager {

    private static Storage storage = new LocalStorage("fs");

    /**
     * Generate file key based on parameters.
     * Currently based on transactionId
     * @param id
     * @return
     */
    public static String generateKey(long id) {
        return String.valueOf(id);
    }

    public static Storage getStorage() {
        return storage;
    }

}
