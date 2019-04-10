/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.DATA_TAG_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.GENESIS_PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PRUNABLE_MESSAGE_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.SHUFFLING_DATA_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TAGGED_DATA_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;

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
        switch (helperTableName.toUpperCase()) {
            case BLOCK_TABLE_NAME :
            case TRANSACTION_TABLE_NAME : {
                if (!relink) {
                    return Optional.of(new BlockTransactionInsertHelper());
                } else {
                    return Optional.of(new RelinkingToSnapshotBlockHelper());
                }
            }
            case GENESIS_PUBLIC_KEY_TABLE_NAME :
            case PUBLIC_KEY_TABLE_NAME :
            case TAGGED_DATA_TABLE_NAME :
            case SHUFFLING_DATA_TABLE_NAME :
            case DATA_TAG_TABLE_NAME :
            case PRUNABLE_MESSAGE_TABLE_NAME :
                return Optional.of(new RelinkingToSnapshotBlockHelper());
            case BLOCK_INDEX_TABLE_NAME :
            case TRANSACTION_SHARD_INDEX_TABLE_NAME : {
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
        if (BLOCK_TABLE_NAME.equals(helperTableName.toUpperCase())) {
            return Optional.of(new BlockDeleteHelper());
        } else {
            throw new IllegalArgumentException("Incorrect Table name was supplied: " + helperTableName);
        }
    }
}
