/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

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
     Optional<T> createSelectInsertHelper(String helperTableName);

     Optional<BatchedPaginationOperation> createSelectInsertHelper(String helperTableName, boolean relink);

    /**
     * Create specified helper class. There are helpers used for deleting
     * @param helperTableName
     * @param helperTableName table name
     * @return table specific helper class OR Empty
     */
    Optional<T> createDeleteHelper(String helperTableName);

}