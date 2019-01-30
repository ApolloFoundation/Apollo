package com.apollocurrency.aplwallet.apl.util.env.config;

/**
 * Loads config for application
 * @param <T> identify type of the loaded config
 */
public interface ConfigLoader<T> {
    /**
     * @return Loaded config
     */
    T load();
}
