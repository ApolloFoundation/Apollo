/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedPaginationOperation> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchedPaginationOperation createSelectInsertHelper(String helperTableName) throws IllegalArgumentException {
        switch (helperTableName.toLowerCase()) {
            case ShardConstants.BLOCK_TABLE_NAME:
            case ShardConstants.TRANSACTION_TABLE_NAME: {
                return (new BlockTransactionInsertHelper());
            }
            case ShardConstants.BLOCK_INDEX_TABLE_NAME:
            case ShardConstants.TRANSACTION_INDEX_TABLE_NAME: {
                return (new SecondaryIndexInsertHelper());
            }
            default: {
                throw new IllegalArgumentException("Incorrect Table name was supplied: " + helperTableName);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchedPaginationOperation createDeleteHelper(String helperTableName) {
        if (ShardConstants.BLOCK_TABLE_NAME.equals(helperTableName.toLowerCase()) || ShardConstants.TRANSACTION_TABLE_NAME.equals(helperTableName.toLowerCase())) {
            return new BlockDeleteHelper();
        } else {
            throw new IllegalArgumentException("Incorrect Table name was supplied: " + helperTableName);
        }
    }
}
