/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShardingHashCalculator {
    private static final Logger log = LoggerFactory.getLogger(ShardingHashCalculator.class);

    private static final int BLOCK_LIMIT = 100;
    private Blockchain blockchain;
    private String algorithm;

    @Inject
    public ShardingHashCalculator(Blockchain blockchain, String algorithm) {
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain cannot be null");
        this.algorithm = StringValidator.requireNonBlank(algorithm, "algorithm");
    }

    private byte[] doHashCalculation(int shardStartHeight, int shardEndHeight) {
        List<byte[]> blockSignatures = blockchain.getBlockSignaturesFrom(shardStartHeight, shardEndHeight, BLOCK_LIMIT);
        List<byte[]> allBlockSignatures = new ArrayList<>();
        int heightOffset = 0;
        while (blockSignatures.size() > 0) {
            allBlockSignatures.addAll(blockSignatures);
            heightOffset += 100;
            blockSignatures = blockchain.getBlockSignaturesFrom(shardStartHeight + heightOffset, shardEndHeight, BLOCK_LIMIT);
        }
        UpdatableMerkleTree tree = new UpdatableMerkleTree(createMessageDigest(), allBlockSignatures);
        return tree.getRoot().getValue();
    }

    private MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create message digest for algo - " + algorithm, e);
        }
    }
    public byte[] calculateHash(int shardStartHeight, int shardEndHeight) {
        if (shardStartHeight >= shardEndHeight) {
            throw new IllegalArgumentException("shard start height should be less than shard end height");
        }
        long startTime = System.currentTimeMillis();
        byte[] hash = doHashCalculation(shardEndHeight, shardStartHeight);
        long time = System.currentTimeMillis() - startTime;
        log.info("Hash calculated in {}s, speed {} bpms", time / 1000, (shardEndHeight - shardStartHeight) / Math.max(time, 1));
        return hash;
    }
}
