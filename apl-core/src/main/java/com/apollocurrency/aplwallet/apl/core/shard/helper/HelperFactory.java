/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

/**
 * Factory for creation helper used for processing specified table
 * @param <T>
 *
 * @author yuriy.larin
 */
public interface HelperFactory<T> {

    /**
     * Create specified helper class. There are helpers used for select-insert, update.
     *
     * @param helperTableName table name
     * @return table specific helper class OR Empty
     */
     T createSelectInsertHelper(String helperTableName);


    /**
     * Create specified helper class. There are helpers used for deleting
     * @param helperTableName table name
     * @return table specific helper class OR Empty
     */
    T createDeleteHelper(String helperTableName);

}