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

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.enterprise.inject.spi.CDI;

public final class BlockImpl implements Block {
    private static final Logger LOG = getLogger(BlockImpl.class);


    private static TransactionDao transactionDb = CDI.current().select(TransactionDaoImpl.class).get();
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static BlockDao blockDao = CDI.current().select(BlockDaoImpl.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountATM;
    private final long totalFeeATM;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private final int timeout;
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

    BlockImpl(byte[] generatorPublicKey, byte[] generationSignature) {
        this(-1, 0, 0, 0, 0, 0, new byte[32], generatorPublicKey, generationSignature, new byte[64],
                new byte[32],  0, Collections.emptyList());
        this.height = 0;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength, byte[] payloadHash,
                     byte[] generatorPublicKey, byte[] generationSignature, byte[] previousBlockHash, int timeout,
                     List<Transaction> transactions,
                     byte[] keySeed) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, null, previousBlockHash, timeout, transactions);
        blockSignature = Crypto.sign(bytes(), keySeed);
        bytes = null;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, int timeout,
              List<Transaction> transactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash, BigInteger.ZERO, blockchainConfig.getCurrentConfig().getInitialBaseTarget(), 0L, -1, 0, timeout, transactions);
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, int timeout,
                     List<Transaction> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorId, null, generationSignature, blockSignature, previousBlockHash,
                cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, blockTransactions);
    }
    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, int timeout,
                     List<Transaction> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash, 0, generatorPublicKey, generationSignature, blockSignature, previousBlockHash,
                cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, blockTransactions);
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, int timeout,
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
        this.baseTarget = baseTarget;
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
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
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
        if (this.blockTransactions == null) {
            List<Transaction> transactions = Collections.unmodifiableList(transactionDb.findBlockTransactions(getId()));
            for (Transaction transaction : transactions) {
                transaction.setBlock(this);
            }
            this.blockTransactions = transactions;
        }
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
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
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
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Long.toUnsignedString(previousBlockId));
        json.put("totalAmountATM", totalAmountATM);
        json.put("totalFeeATM", totalFeeATM);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        json.put("blockSignature", Convert.toHexString(blockSignature));
        json.put("timeout", timeout);

        JSONArray transactionsData = new JSONArray();
        getTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        return json;
    }

    static BlockImpl parseBlock(JSONObject blockData) throws AplException.NotValidException {
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
            int timeout =  !requireTimeout(version) ? 0 : ((Long) timeoutJsonValue).intValue();
            List<Transaction> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountATM, totalFeeATM, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, timeout, blockTransactions);
            if (!block.checkSignature()) {
                throw new AplException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (AplException.NotValidException|RuntimeException e) {
            LOG.debug("Failed to parse block: " + blockData.toJSONString());
            LOG.debug("Exception: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    static boolean requireTimeout(int version) {
        return Block.ADAPTIVE_BLOCK_VERSION == version || Block.INSTANT_BLOCK_VERSION == version;
    }
    byte[] bytes() {
        if (bytes == null) {
            ByteBuffer buffer =
                    ByteBuffer.allocate(4 + 4 + 8 + 4 + 8 + 8 + 4 + 32 + 32 + 32 + 32 +
                            (requireTimeout(version) ? 4 : 0) + (blockSignature != null ? 64 :
                    0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(getTransactions().size());
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
    public boolean verifyBlockSignature() {
        return checkSignature() && Account.setOrVerify(getGeneratorId(), getGeneratorPublicKey());
    }

    private volatile boolean hasValidSignature = false;


    private boolean checkSignature() {
        if (! hasValidSignature) {
            byte[] data = Arrays.copyOf(bytes(), bytes.length - 64);
            hasValidSignature = blockSignature != null && Crypto.verify(blockSignature, data, getGeneratorPublicKey());
        }
        return hasValidSignature;
    }

    @Override
    public boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {

        try {

            Block previousBlock = blockchain.getBlock(getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", this);
            }

            Account account = Account.getAccount(getGeneratorId());
            long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceAPL();
            if (effectiveBalance <= 0) {
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            digest.update(previousBlock.getGenerationSignature());
            byte[] generationSignatureHash = digest.digest(getGeneratorPublicKey());
            if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                return false;
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            return Generator.verifyHit(hit, BigInteger.valueOf(effectiveBalance), previousBlock, requireTimeout(version) ? timestamp - timeout: timestamp);

        } catch (RuntimeException e) {

            LOG.info("Error verifying block generation signature", e);
            return false;

        }

    }

    void apply() {
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(getGeneratorPublicKey());
        long totalBackFees = 0;
        if (this.height > 3) {
            long[] backFees = new long[3];
            for (Transaction transaction : getTransactions()) {
                long[] fees = ((TransactionImpl)transaction).getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                Account previousGeneratorAccount = Account.getAccount(blockDao.findBlockAtHeight(this.height - i - 1).getGeneratorId());
                LOG.debug("Back fees {} {} to forger at height {}", ((double)backFees[i])/Constants.ONE_APL, blockchainConfig.getCoinSymbol(),
                        this.height - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceATM(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            LOG.debug("Fee reduced by {} {} at height {}", ((double)totalBackFees)/Constants.ONE_APL, blockchainConfig.getCoinSymbol(), this.height);
        }
        generatorAccount.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.BLOCK_GENERATED, getId(), totalFeeATM - totalBackFees);
        generatorAccount.addToForgedBalanceATM(totalFeeATM - totalBackFees);
    }

    @Override
    public void setPrevious(Block block) {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (Transaction transaction : getTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    void loadTransactions() {
        for (Transaction transaction : getTransactions()) {
            ((TransactionImpl)transaction).bytes();
            transaction.getAppendages();
        }
    }


    private void calculateBaseTarget(Block previousBlock) {
        long prevBaseTarget = previousBlock.getBaseTarget();
        int blockchainHeight = previousBlock.getHeight();
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            int blocktimeAverage = getBlockTimeAverage(previousBlock);
            HeightConfig config = blockchainConfig.getCurrentConfig();
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

    private int getBlockTimeAverage(Block previousBlock) {
        int blockchainHeight = previousBlock.getHeight();
        Block lastBlockForTimeAverage = blockDao.findBlockAtHeight(blockchainHeight - 2);
        if (version != Block.LEGACY_BLOCK_VERSION) {
            Block intermediateBlockForTimeAverage = blockDao.findBlockAtHeight(blockchainHeight - 1);
            int thisBlockActualTime = this.timestamp - previousBlock.getTimestamp() - this.timeout;
            int previousBlockTime = previousBlock.getTimestamp() - previousBlock.getTimeout() - intermediateBlockForTimeAverage.getTimestamp();
            int secondAvgBlockTime = intermediateBlockForTimeAverage.getTimestamp()
                    - intermediateBlockForTimeAverage.getTimeout() - lastBlockForTimeAverage.getTimestamp();
            return  (thisBlockActualTime + previousBlockTime + secondAvgBlockTime) / 3;
        } else {
            return (this.timestamp - lastBlockForTimeAverage.getTimestamp()) / 3;
        }
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
