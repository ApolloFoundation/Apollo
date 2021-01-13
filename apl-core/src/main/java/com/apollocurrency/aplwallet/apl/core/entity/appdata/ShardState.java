/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import lombok.extern.slf4j.Slf4j;

/**
 * Shard state enum used by shard record in db.
 * We want distinguish shard state by existing different data available from current node.
 */
@Slf4j
public enum ShardState {

    /**
     * Prepare shard creation. It is prepared to start, but not finished yet.
     * OR
     * That means there are NO exists block/transaction data, no ZIP shard data archive for shard in that state.
     */
    INIT(0),

    /**
     * Process of shard creation was started, but it's in the middle of process
     */
    IN_PROGRESS(1),

    /**
     * Shard was created from importing 'shard arcvhie', so it potentially has ONLY zipped shard and not real block/transaction data inside.
     */
    CREATED_BY_ARCHIVE(50),

    /**
     * Shard was created from real data and it has as ZIP created + real block/transaction data in shard db.
     */
    FULL(100);

    private final long internalStateValue; // internal value for compare operation

    ShardState(long id) {
        this.internalStateValue = id;
    }

    public static ShardState getType(long ordinal) {
        if (ordinal < 0 || ordinal > FULL.getValue()) {
            log.error("-> ERROR!!! Unmapped/incorrect 'ShardState' value from DB ?? = {}", ordinal);
            return null;
        }
        switch ((int) ordinal) {
            case 0:
                return INIT;
            case 1:
                return IN_PROGRESS;
            case 50:
                return CREATED_BY_ARCHIVE;
            case 100:
                return FULL;
            default:
                throw new RuntimeException("No correct ENUM was found for supplied long value = " + ordinal);
        }
    }

    public long getValue() {
        return internalStateValue;
    }

}
