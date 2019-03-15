/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedPaginationOperation> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedPaginationOperation> createSelectInsertHelper(String helperTableName) {
        Optional<BatchedPaginationOperation> helper;
        switch (helperTableName.toUpperCase()) {
            case "BLOCK" : {
                return Optional.of(new BlockSelectAndInsertHelper());
            }
            case "TRANSACTION" : {
                return Optional.of(new TransactionSelectAndInsertHelper());
            }
            case "GENESIS_PUBLIC_KEY" :
            case "PUBLIC_KEY" :
            case "TAGGED_DATA" :
            case "SHUFFLING_DATA" :
            case "DATA_TAG" :
            case "PRUNABLE_MESSAGE" :
                return Optional.of(new RelinkingToSnapshotBlockHelper());
            case "BLOCK_INDEX" :
            case "TRANSACTION_SHARD_INDEX" : {
                return Optional.of(new SecondaryIndexSelectAndInsertHelper());
            }
            default:
                helper = Optional.empty();
        }
        return helper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedPaginationOperation> createDeleteHelper(String helperTableName) {
        Optional<BatchedPaginationOperation> helper;
        if ("BLOCK".equals(helperTableName.toUpperCase())) {
            return Optional.of(new BlockDeleteHelper());
        } else {
            helper = Optional.empty();
        }
        return helper;
    }
}
