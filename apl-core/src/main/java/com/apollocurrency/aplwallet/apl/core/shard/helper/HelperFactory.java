/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

/**
 * Factory for creation helper used for processing specified table
 *
 * @param <T>
 * @author yuriy.larin
 */
public interface HelperFactory<T> {

    /**
     * Create specified helper class. There are helpers used for select-insert, update.
     *
     * @param helperTableName table name
     * @return table specific helper class OR throw exception
     * @throws IllegalArgumentException when unable to find helper for specific helperTableName
     */
    T createSelectInsertHelper(String helperTableName) throws IllegalArgumentException;

    /**
     * Create specified helper class. There are helpers used for deleting
     *
     * @param helperTableName
     * @param helperTableName table name
     * @return table specific helper class OR throw exception
     * @throws IllegalArgumentException when unable to find helper for specific table
     */
    T createDeleteHelper(String helperTableName);

}