/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.fs;

public interface Storage {

    boolean put(String key, byte[] data);
    byte[] get(String key);
    void delete(String key);

}
