/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;

import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedPaginationOperation> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedPaginationOperation> createSelectInsertHelper(String helperTableName, boolean relink) {
        Optional<BatchedPaginationOperation> helper;
        switch (helperTableName.toLowerCase()) {
            case ShardConstants.BLOCK_TABLE_NAME :
            case ShardConstants.TRANSACTION_TABLE_NAME : {
                if (!relink) {
                    return Optional.of(new BlockTransactionInsertHelper());
                } else {
                    return Optional.of(new SecondaryIndexInsertHelper());
                }
            }
/*
            case GENESIS_PUBLIC_KEY_TABLE_NAME :
            case PUBLIC_KEY_TABLE_NAME :
            case TAGGED_DATA_TABLE_NAME :
            case SHUFFLING_DATA_TABLE_NAME :
            case DATA_TAG_TABLE_NAME :
            case PRUNABLE_MESSAGE_TABLE_NAME :
                return Optional.of(new RelinkingToSnapshotBlockHelper());
*/
            case ShardConstants.BLOCK_INDEX_TABLE_NAME :
            case ShardConstants.TRANSACTION_INDEX_TABLE_NAME : {
                return Optional.of(new SecondaryIndexInsertHelper());
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
    public Optional<BatchedPaginationOperation> createSelectInsertHelper(String helperTableName) {
        return createSelectInsertHelper(helperTableName, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedPaginationOperation> createDeleteHelper(String helperTableName) {
        if (ShardConstants.BLOCK_TABLE_NAME.equals(helperTableName.toLowerCase()) || ShardConstants.TRANSACTION_TABLE_NAME.equals(helperTableName.toLowerCase())) {
            return Optional.of(new BlockDeleteHelper());
        } else {
            throw new IllegalArgumentException("Incorrect Table name was supplied: " + helperTableName);
        }
    }
}
