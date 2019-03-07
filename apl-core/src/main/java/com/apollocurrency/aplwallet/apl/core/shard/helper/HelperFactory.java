package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

/**
 * Factory for creation helper used for processing specified table
 * @param <T>
 */
public interface HelperFactory<T> {

    /**
     * Create specified helper class
     *
     * @param helperTableName table name
     * @return table specific helper class OR NULL
     */
        Optional<T> createHelper(String helperTableName);

}
