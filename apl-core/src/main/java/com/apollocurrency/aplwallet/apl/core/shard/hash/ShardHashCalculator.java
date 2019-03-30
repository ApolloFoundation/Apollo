/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

/**
 * Calculate hash for shard data
 */
public interface ShardHashCalculator {
    /**
     * Calculate hash for shard data from shardStartHeight to shardEndHeight
     * @param shardStartHeight height of data from which hash calculation will be started (inclusive)
     * @param shardEndHeight height of data where hash calculation will be finished (exclusive)
     * @return calculated hash or null, when there is not enough shard data between shardStartHeight and shardEndHeight
     */
    byte[] calculateHash(int shardStartHeight, int shardEndHeight);
}
