/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShardingHashCalculator {
    private static final Logger log = LoggerFactory.getLogger(ShardingHashCalculator.class);

    private static final int BLOCK_LIMIT = 100;
    private Blockchain blockchain;

    @Inject
    public ShardingHashCalculator(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public byte[] calculateHash(int shardStartHeight) {
        long blockId = blockchain.getBlockIdAtHeight(shardStartHeight);
        List<Long> blockIdsAfter = blockchain.getBlockIdsAfter(blockId, BLOCK_LIMIT);
        MessageDigest digest = Crypto.sha256();
        long start = System.currentTimeMillis();
        int blocks = 0;
        while (blockIdsAfter.size() > 0) {
            for (Long id : blockIdsAfter) {
                digest.update(longToBytes(id));
            }
            blocks += blockIdsAfter.size();
            if (blocks % 100_000 == 0) {
                log.info("Processed {} blocks for shard hash", blocks);
            }
            blockIdsAfter = blockchain.getBlockIdsAfter(blockIdsAfter.get(blockIdsAfter.size() - 1), BLOCK_LIMIT);
        }
        long time = System.currentTimeMillis() - start;
        log.info("Hash calculated in {}s, speed {} bpms", time / 1000, blocks / time);
        return digest.digest();
    }
    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
}
