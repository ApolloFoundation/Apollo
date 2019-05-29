/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_SHARD_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedPaginationOperation> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BatchedPaginationOperation createSelectInsertHelper(String helperTableName) {
        switch (helperTableName.toUpperCase()) {
            case BLOCK_TABLE_NAME :
            case TRANSACTION_TABLE_NAME : {
                    return new BlockTransactionInsertHelper();
            }
            case BLOCK_INDEX_TABLE_NAME :
            case TRANSACTION_SHARD_INDEX_TABLE_NAME : {
                return new SecondaryIndexInsertHelper();
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
        if (BLOCK_TABLE_NAME.equals(helperTableName.toUpperCase()) || TRANSACTION_TABLE_NAME.equals(helperTableName.toUpperCase())) {
            return new BlockDeleteHelper();
        } else {
            throw new IllegalArgumentException("Incorrect Table name was supplied: " + helperTableName);
        }
    }
}
