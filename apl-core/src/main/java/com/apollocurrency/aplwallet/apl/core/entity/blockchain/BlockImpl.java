/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private final byte[] previousBlockHash;
    private final long totalAmountATM;
    private final long totalFeeATM;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private final int timeout;
    private volatile byte[] generatorPublicKey;
    private volatile List<Transaction> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty;
    private long baseTarget;
    private volatile long nextBlockId;
    private int height;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private volatile byte[] bytes = null;
    private volatile boolean hasValidSignature = false;

    public BlockImpl(byte[] generatorPublicKey, byte[] generationSignature, long baseTarget) {
        this(-1, 0, 0, 0, 0, 0, new byte[32], generatorPublicKey,
            generationSignature, new byte[64],
            new byte[32], 0, Collections.emptyList(), baseTarget);
        this.height = 0;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength, byte[] payloadHash,
                     byte[] generatorPublicKey, byte[] generationSignature, byte[] previousBlockHash, int timeout,
                     List<Transaction> transactions,
                     byte[] keySeed,
                     long baseTarget) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash,
            generatorPublicKey, generationSignature, null, previousBlockHash, timeout, transactions, baseTarget);
        blockSignature = Crypto.sign(bytes(), keySeed);
        bytes = null;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength, byte[] payloadHash,
                     byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, int timeout,
                     List<Transaction> transactions, long baseTarget) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorPublicKey,
            generationSignature, blockSignature, previousBlockHash, BigInteger.ZERO, baseTarget, 0L, -1, 0, timeout,
            transactions);
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, BigInteger cumulativeDifficulty, Long baseTarget, long nextBlockId, int height, long id, int timeout,
                     List<Transaction> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorId, null, generationSignature, blockSignature, previousBlockHash,
            cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, blockTransactions);
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, BigInteger cumulativeDifficulty, Long baseTarget, long nextBlockId, int height, long id, int timeout,
                     List<Transaction> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, 0, generatorPublicKey, generationSignature, blockSignature, previousBlockHash,
            cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, blockTransactions);
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, long generatorId, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, BigInteger cumulativeDifficulty, Long baseTarget, long nextBlockId, int height, long id, int timeout,
                     List<Transaction> blockTransactions) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountATM = totalAmountATM;
        this.totalFeeATM = totalFeeATM;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        this.timeout = timeout;
        this.cumulativeDifficulty = cumulativeDifficulty;
        if (baseTarget != null) {
            this.baseTarget = baseTarget;
        } else {
            String error = "'baseTarget' can't be null or empty ! Supply it from 'config' data, pls...";
            throw new RuntimeException(error);
        }
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        if (generatorPublicKey != null) {
            this.generatorPublicKey = generatorPublicKey;
            this.generatorId = Convert.getId(generatorPublicKey);
        } else {
            this.generatorId = generatorId;
        }
        if (blockTransactions != null) {
            this.blockTransactions = Collections.unmodifiableList(blockTransactions);
        }
    }

    static boolean requireTimeout(int version) {
        return Block.ADAPTIVE_BLOCK_VERSION == version || Block.INSTANT_BLOCK_VERSION == version;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public void setGeneratorPublicKey(byte[] generatorPublicKey) {
        if (generatorPublicKey != null && generatorPublicKey.length > 0) {
            this.generatorPublicKey = generatorPublicKey;
        } else {
            String error = "Can't assign empty generatorPublicKey";
            throw new RuntimeException(error);
        }
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountATM() {
        return totalAmountATM;
    }

    @Override
    public long getTotalFeeATM() {
        return totalFeeATM;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<Transaction> getTransactions() {
        return this.blockTransactions;
    }

    @Override
    public void setTransactions(List<Transaction> transactions) {
        this.blockTransactions = Objects.requireNonNull(transactions, "transaction List should not be NULL");
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    public void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
//            assuming that calculation of id will work only for generated blocks
            byte[] hash = Crypto.sha256().digest(bytes());
            BigInteger bigInteger = new BigInteger(1, new byte[]{hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Long.toUnsignedString(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
//            generatorId = AccountService.getId(getGeneratorPublicKey());
            String error = "GeneratorId should be assigned !";
            throw new RuntimeException(error);
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl) o).getId();
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    public byte[] bytes() {
        if (bytes == null) {
            ByteBuffer buffer =
                ByteBuffer.allocate(4 + 4 + 8 + 4 + 8 + 8 + 4 + 32 + 32 + 32 + 32 +
                    (requireTimeout(version) ? 4 : 0) + (blockSignature != null ? 64 :
                    0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(blockTransactions != null ? blockTransactions.size() : /*getOrLoadTransactions().size()*/ 0);
            buffer.putLong(totalAmountATM);
            buffer.putLong(totalFeeATM);
            buffer.putInt(payloadLength);
            buffer.put(payloadHash);
            buffer.put(getGeneratorPublicKey());
            buffer.put(generationSignature);
            buffer.put(previousBlockHash);
            if (requireTimeout(version)) {
                buffer.putInt(timeout);
            }
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            bytes = buffer.array();
        }
        return bytes;
    }

    @Override
    public boolean checkSignature() {
        if (!hasValidSignature) {
            byte[] data = Arrays.copyOf(bytes(), bytes.length - 64);
            hasValidSignature = blockSignature != null && Crypto.verify(blockSignature, data, getGeneratorPublicKey());
        }
        return hasValidSignature;
    }

    @Override
    public void setPrevious(Block[] threeLatestBlocksArray, HeightConfig config, Shard lastShard, int initialBlockHeight) {
        Objects.requireNonNull(config, "HeightConfig is NULL");
        Objects.requireNonNull(threeLatestBlocksArray, "shardInitialBlock is NULL");// check for now (not sure if needed?)
        if (threeLatestBlocksArray.length == 0) {
            // shouldn't happen as previous id is already verified, but just in case
            throw new IllegalArgumentException("threeLatestBlocksArray is empty and has 0 element(s)");
        }
        if (threeLatestBlocksArray[0] != null) {
            if (threeLatestBlocksArray[0].getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous threeLatestBlocksArray id doesn't match");
            }
            this.height = threeLatestBlocksArray[0].getHeight() + 1;
            this.calculateBaseTarget(threeLatestBlocksArray, config, lastShard, initialBlockHeight);
            short index = 0;
            for (Transaction transaction : this.blockTransactions) {
                transaction.setBlock(this);
                transaction.setIndex(index++);
                ((TransactionImpl) transaction).bytes();
                transaction.getAppendages();
            }
        } else {
            this.height = 0;
        }
    }

    public void assignTransactionsIndex() {
        // important !!! assign transaction index value
        short index = 0;
        for (Transaction transaction : this.blockTransactions) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
            ((TransactionImpl) transaction).bytes();
            transaction.getAppendages();
        }
    }

    private void calculateBaseTarget(Block[] threeLatestBlocksArray, HeightConfig config, Shard lastShard, int initialBlockHeight) {
        long prevBaseTarget = threeLatestBlocksArray[0].getBaseTarget();
        int blockchainHeight = threeLatestBlocksArray[0].getHeight();
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            int blocktimeAverage = getBlockTimeAverage(threeLatestBlocksArray, lastShard, initialBlockHeight);
            int blockTime = config.getBlockTime();
            if (blocktimeAverage > blockTime) {
                int maxBlocktimeLimit = config.getMaxBlockTimeLimit();
                this.baseTarget = (prevBaseTarget * Math.min(blocktimeAverage, maxBlocktimeLimit)) / blockTime;
            } else {
                int minBlocktimeLimit = config.getMinBlockTimeLimit();
                this.baseTarget = prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                    * (blockTime - Math.max(blocktimeAverage, minBlocktimeLimit)) / (100 * blockTime);
            }
            long maxBaseTarget = config.getMaxBaseTarget();
            if (this.baseTarget < 0 || this.baseTarget > maxBaseTarget) {
                this.baseTarget = maxBaseTarget;
            }
            long minBaseTarget = config.getMinBaseTarget();
            if (this.baseTarget < minBaseTarget) {
                this.baseTarget = config.getMinBaseTarget();
            }
        } else {
            this.baseTarget = prevBaseTarget;
        }
        this.cumulativeDifficulty = threeLatestBlocksArray[0].getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(this.baseTarget)));
    }

    private int getBlockTimeAverage(Block[] threeLatestBlocksArray, Shard lastShard, int initialBlockHeight) {
        int blockchainHeight = threeLatestBlocksArray[0].getHeight();
        Block blockAtHeight = threeLatestBlocksArray[2];
        int lastBlockTimestamp = validatePrevTimestamp(
            initialBlockHeight, blockchainHeight - 2, lastShard, blockAtHeight);
        if (this.version != Block.LEGACY_BLOCK_VERSION) {
            blockAtHeight = threeLatestBlocksArray[1];
            int intermediateTimestamp = validatePrevTimestamp(initialBlockHeight, blockchainHeight - 1, lastShard, blockAtHeight);
            int intermediateTimeout = validatePrevTimeout(initialBlockHeight, blockchainHeight - 1, lastShard, blockAtHeight);
            int thisBlockActualTime = this.timestamp - threeLatestBlocksArray[0].getTimestamp() - this.timeout;
            int previousBlockTime = threeLatestBlocksArray[0].getTimestamp() - threeLatestBlocksArray[0].getTimeout() - intermediateTimestamp;
            int secondAvgBlockTime = intermediateTimestamp
                - intermediateTimeout - lastBlockTimestamp;
            return (thisBlockActualTime + previousBlockTime + secondAvgBlockTime) / 3;
        } else {
            return (this.timestamp - lastBlockTimestamp) / 3;
        }
    }

    private int validatePrevTimestamp(int shardInitialHeight, int blockHeight, Shard lastShard, Block blockAtHeight) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timestamp for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimestamps = lastShard.getBlockTimestamps();
            return blockTimestamps[diff - 1];
        }
        return blockAtHeight.getTimestamp();
    }

    private int validatePrevTimeout(int shardInitialHeight, int blockHeight, Shard lastShard, Block blockAtHeight) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timeout for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimeouts = lastShard.getBlockTimeouts();
            return blockTimeouts[diff - 1];
        }
        return blockAtHeight.getTimeout();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BlockImpl{");
        sb.append("version=").append(version);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", previousBlockId=").append(previousBlockId);
        sb.append(", totalAmountATM=").append(totalAmountATM);
        sb.append(", totalFeeATM=").append(totalFeeATM);
        sb.append(", timeout=").append(timeout);
        sb.append(", blockTransactions=[").append(blockTransactions != null ? blockTransactions.size() : -1);
        sb.append("], baseTarget=").append(baseTarget);
        sb.append(", nextBlockId=").append(nextBlockId);
        sb.append(", height=").append(height);
        sb.append(", stringId='").append(stringId).append('\'');
        sb.append(", generatorId=").append(generatorId);
        sb.append(", hasValidSignature=").append(hasValidSignature);
        sb.append('}');
        return sb.toString();
    }
}
