/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
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

    private static final int DEFAULT_BLOCK_LIMIT = 100;
    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private int blockSelectLimit;
    @Inject
    public ShardingHashCalculator(Blockchain blockchain, BlockchainConfig blockchainConfig) {
        this(blockchain, blockchainConfig, DEFAULT_BLOCK_LIMIT);
    }

    public ShardingHashCalculator(Blockchain blockchain, BlockchainConfig blockchainConfig, int blockSelectLimit) {
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain cannot be null");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, " blockchainConfig");
        if (blockSelectLimit <= 0) {
            throw new IllegalArgumentException("blockSelect should be positive");
        }
        this.blockSelectLimit = blockSelectLimit;
    }

    private byte[] doHashCalculation(int shardStartHeight, int shardEndHeight) {
        List<byte[]> blockSignatures = blockchain.getBlockSignaturesFrom(shardStartHeight, shardEndHeight, blockSelectLimit);
        List<byte[]> allBlockSignatures = new ArrayList<>();
        int heightOffset = 0;
        while (shardStartHeight + heightOffset <= shardEndHeight) {
            allBlockSignatures.addAll(blockSignatures);
            heightOffset += blockSelectLimit;
            blockSignatures = blockchain.getBlockSignaturesFrom(shardStartHeight + heightOffset, shardEndHeight, blockSelectLimit);
        }
        MerkleTree tree = new MerkleTree(createMessageDigest(), allBlockSignatures);
        return tree.getRoot().getValue();
    }

    private MessageDigest createMessageDigest() {
        String algorithm = blockchainConfig.getCurrentConfig().getShardingDigestAlgorithm();
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
