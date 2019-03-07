package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class HelperFactoryImpl implements HelperFactory<BatchedSelectInsert> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BatchedSelectInsert> createHelper(String helperTableName) {
        if ("BLOCK".equalsIgnoreCase(helperTableName)) {
            return Optional.of(new BlockSelectAndInsertHelper());
        } else if ("TRANSACTION".equalsIgnoreCase(helperTableName)) {
            return Optional.of(new TransactionSelectAndInsertHelper());
        }
        return Optional.empty();
    }
}
