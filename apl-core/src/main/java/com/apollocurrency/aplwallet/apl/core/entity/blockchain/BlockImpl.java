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

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

public final class BlockImpl implements Block {
    private static final Logger LOG = getLogger(BlockImpl.class);

    private static Blockchain blockchain;

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

    public static BlockImpl parseBlock(JSONObject blockData, long baseTarget) throws AplException.NotValidException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountATM = blockData.containsKey("totalAmountATM") ? Convert.parseLong(blockData.get("totalAmountATM")) : Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeATM = blockData.containsKey("totalFeeATM") ? Convert.parseLong(blockData.get("totalFeeATM")) : Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            Object timeoutJsonValue = blockData.get("timeout");
            int timeout = !requireTimeout(version) ? 0 : ((Long) timeoutJsonValue).intValue();
            List<Transaction> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionBuilder.parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountATM, totalFeeATM,
                payloadLength, payloadHash, generatorPublicKey,
                generationSignature, blockSignature, previousBlockHash, timeout, blockTransactions, baseTarget);
            if (!block.checkSignature()) {
                throw new AplException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse block: " + blockData.toJSONString());
            LOG.debug("Exception: " + e.getMessage());
            throw e;
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
//        if (generatorPublicKey == null) {
//            String error = "Pls, assign generatorPublicKey first, before trying to take it";
//            throw new RuntimeException(error);
//            generatorPublicKey = lookupAccountService().getPublicKeyByteArray(generatorId);
//        }
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
    public List<Transaction> getOrLoadTransactions() {
        if (this.blockTransactions == null) {
            List<Transaction> transactions = Collections.unmodifiableList(lookupBlockchain().getBlockTransactions(getId()));
            for (Transaction transaction : transactions) {
                transaction.setBlock(this);
            }
            this.blockTransactions = transactions;
        }
        return this.blockTransactions;
    }

    @Override
    public List<Transaction> getTransactions() {
        return this.blockTransactions;
    }

    @Override
    public void setTransactions(List<Transaction> transactions) {
        this.blockTransactions = transactions;
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
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("stringId", stringId);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Long.toUnsignedString(previousBlockId));
        json.put("totalAmountATM", totalAmountATM);
        json.put("totalFeeATM", totalFeeATM);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorId", Long.toUnsignedString(generatorId));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        json.put("blockSignature", Convert.toHexString(blockSignature));
        json.put("timeout", timeout);

        JSONArray transactionsData = new JSONArray();
        getOrLoadTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        return json;
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
            buffer.putInt(getOrLoadTransactions().size());
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
    public void setPrevious(Block block, HeightConfig config, Shard lastShard) {
        Objects.requireNonNull(config, "HeightConfig is NULL");
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block, config, lastShard);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (Transaction transaction : getOrLoadTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    public void loadTransactions() {
        for (Transaction transaction : getOrLoadTransactions()) {
            transaction.bytes();
            transaction.getAppendages();
        }
    }

    private void calculateBaseTarget(Block previousBlock, HeightConfig config, Shard lastShard) {
        long prevBaseTarget = previousBlock.getBaseTarget();
        int blockchainHeight = previousBlock.getHeight();
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            int blocktimeAverage = getBlockTimeAverage(previousBlock, lastShard);
            int blockTime = config.getBlockTime();
            if (blocktimeAverage > blockTime) {
                int maxBlocktimeLimit = config.getMaxBlockTimeLimit();
                baseTarget = (prevBaseTarget * Math.min(blocktimeAverage, maxBlocktimeLimit)) / blockTime;
            } else {
                int minBlocktimeLimit = config.getMinBlockTimeLimit();
                baseTarget = prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                    * (blockTime - Math.max(blocktimeAverage, minBlocktimeLimit)) / (100 * blockTime);
            }
            long maxBaseTarget = config.getMaxBaseTarget();
            if (baseTarget < 0 || baseTarget > maxBaseTarget) {
                baseTarget = maxBaseTarget;
            }
            long minBaseTarget = config.getMinBaseTarget();
            if (baseTarget < minBaseTarget) {
                baseTarget = config.getMinBaseTarget();
            }
        } else {
            baseTarget = prevBaseTarget;
        }
        cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
    }

    private int getBlockTimeAverage(Block previousBlock, Shard lastShard) {
        int blockchainHeight = previousBlock.getHeight();
        Block shardInitialBlock = lookupBlockchain().getShardInitialBlock();
        int lastBlockTimestamp = getPrevTimestamp(shardInitialBlock.getHeight(), blockchainHeight - 2, lastShard);
        if (version != Block.LEGACY_BLOCK_VERSION) {
            int intermediateTimestamp = getPrevTimestamp(shardInitialBlock.getHeight(), blockchainHeight - 1, lastShard);
            int intermediateTimeout = getPrevTimeout(shardInitialBlock.getHeight(), blockchainHeight - 1, lastShard);
            int thisBlockActualTime = this.timestamp - previousBlock.getTimestamp() - this.timeout;
            int previousBlockTime = previousBlock.getTimestamp() - previousBlock.getTimeout() - intermediateTimestamp;
            int secondAvgBlockTime = intermediateTimestamp
                - intermediateTimeout - lastBlockTimestamp;
            return (thisBlockActualTime + previousBlockTime + secondAvgBlockTime) / 3;
        } else {
            return (this.timestamp - lastBlockTimestamp) / 3;
        }
    }

    private int getPrevTimestamp(int shardInitialHeight, int blockHeight, Shard lastShard) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timestamp for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimestamps = lastShard.getBlockTimestamps();
            return blockTimestamps[diff - 1];
        }
        return lookupBlockchain().getBlockAtHeight(blockHeight).getTimestamp();
    }

    private int getPrevTimeout(int shardInitialHeight, int blockHeight, Shard lastShard) {
        int diff = shardInitialHeight - blockHeight;
        if (diff > 2) {
            throw new IllegalArgumentException("Unable to retrieve block timeout for height " + blockHeight + " current shard height " + shardInitialHeight);
        }
        if (diff > 0) {
            int[] blockTimeouts = lastShard.getBlockTimeouts();
            return blockTimeouts[diff - 1];
        }
        return lookupBlockchain().getBlockAtHeight(blockHeight).getTimeout();
    }

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(Blockchain.class).get();
        }
        return blockchain;
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
