package com.apollocurrency.aplwallet.apl.core.app.service;

import java.util.Properties;

public interface PropertyStorageService {

    String EXPORTED_DATA_FILE_NAME = "exported-data.properties";
    String SS_KEY_NAME = "YXBsLmRwYXM=";
    String PHASH_KEY_NAME = "YXBsLmFkbWluLnBoYXNo";
    String PVAL_KEY_NAME = "YXBsLmFkbWluLnB2YWw=";

    /**
     * Save properties in to store.
     *
     * @return true - if properties were saved successfully, otherwise returned false
     */
    boolean storeProperties(Properties props);

    /**
     * Load properties from store.
     */
    Properties loadProperties();

    /**
     * Is properties store exist.
     *
     * @return true - if properties store exist, otherwise returned false
     */
    boolean isExist();

}
