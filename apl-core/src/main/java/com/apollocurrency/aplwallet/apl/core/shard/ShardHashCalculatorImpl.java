/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

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

/**
 * <p>This implementation uses merkle tree and block signatures for hash calculations</p>
 */
@Singleton
public class ShardHashCalculatorImpl implements ShardHashCalculator {
    private static final Logger log = LoggerFactory.getLogger(ShardHashCalculatorImpl.class);

    private static final int DEFAULT_BLOCK_LIMIT = 100;
    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private int blockSelectLimit;
    @Inject
    public ShardHashCalculatorImpl(Blockchain blockchain, BlockchainConfig blockchainConfig) {
        this(blockchain, blockchainConfig, DEFAULT_BLOCK_LIMIT);
    }

    public ShardHashCalculatorImpl(Blockchain blockchain, BlockchainConfig blockchainConfig, int blockSelectLimit) {
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain cannot be null");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, " blockchainConfig");
        if (blockSelectLimit <= 0) {
            throw new IllegalArgumentException("blockSelect should be positive");
        }
        this.blockSelectLimit = blockSelectLimit;
    }

    private List<byte[]> retrieveBlockSignatures(int shardStartHeight, int shardEndHeight) {
        List<byte[]> allBlockSignatures = new ArrayList<>();
        int fromHeight = shardStartHeight;
        while (fromHeight < shardEndHeight) {
            List<byte[]> blockSignatures  = blockchain.getBlockSignaturesFrom(fromHeight, Math.min(fromHeight + blockSelectLimit, shardEndHeight));
            allBlockSignatures.addAll(blockSignatures);
            fromHeight += blockSelectLimit;
        }
        return allBlockSignatures;
    }

    private byte[] calculateMerkleRoot(List<byte[]> dataList) {
        MerkleTree tree = new MerkleTree(createMessageDigest(), dataList);
        return tree.getRoot() == null ? null : tree.getRoot().getValue();
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

    /**
     * {@inheritDoc}
     * <p>This implementation utilize block signatures as shard data and merkle tree as hashing data structure</p>
     * @return calculated hash or null, when no blocks exist between shardStartHeight(inclusive) and shardEndHeight(exclusive)
     */
    @Override
    public byte[] calculateHash(int shardStartHeight, int shardEndHeight) {
        if (shardStartHeight >= shardEndHeight) {
            throw new IllegalArgumentException("shard start height should be less than shard end height");
        }
        long startTime = System.currentTimeMillis();
        List<byte[]> blockSignatures = retrieveBlockSignatures(shardStartHeight, shardEndHeight);
        log.info("Retrieved {} block signatures in {} ms",blockSignatures.size(), System.currentTimeMillis() - startTime);
        long merkleTreeStartTime = System.currentTimeMillis();
        byte[] hash = calculateMerkleRoot(blockSignatures);
        log.info("Built merkle tree in {} ms", System.currentTimeMillis() - merkleTreeStartTime);
        long time = System.currentTimeMillis() - startTime;
        log.info("Hash calculated in {}s, speed {} bpms", time / 1000, (shardEndHeight - shardStartHeight) / Math.max(time, 1));
        return hash;
    }
}
