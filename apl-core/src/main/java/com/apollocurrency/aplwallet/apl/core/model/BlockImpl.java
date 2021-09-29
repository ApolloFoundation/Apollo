/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.exception.AplBlockTxErrorResultsMismatchException;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

//TODO RawBlock impl (without consensus data)
@Slf4j
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
    private volatile List<Transaction> blockTransactions = Collections.emptyList();
    private volatile List<TxErrorHash> txErrorHashes = Collections.emptyList();

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
    private volatile boolean hasLoadedData = false;

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

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountATM, long totalFeeATM, int payloadLength,
                     byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature,
                     byte[] previousBlockHash, int timeout, List<Transaction> transactions, long baseTarget) {
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
        this.generatorId = generatorId;
        if (blockTransactions != null) {
            assignBlockData(blockTransactions, generatorPublicKey);
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
    public List<TxErrorHash> getTxErrorHashes() {
        return txErrorHashes;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            throw new IllegalStateException("Generator's public key was not set");
        }
        return generatorPublicKey;
    }

    @Override
    public boolean hasGeneratorPublicKey() {
        return generatorPublicKey != null;
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
    public long getBaseTarget() {
        return baseTarget;
    }

    public void setBaseTarget(long baseTarget) {
        this.baseTarget = baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
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

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
//            assuming that calculation of id will work only for generated blocks
            BigInteger bigInteger = AplIdGenerator.BLOCK.getId(bytes());
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
                    (requireTimeout(version) ? 4 : 0)  + (40 * txErrorHashes.size()) + (blockSignature != null ? 64 :
                    0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(blockTransactions.size());
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
            txErrorHashes.forEach(e -> { // only when failed txs are present
                buffer.putLong(e.getId());
                buffer.put(e.getErrorHash());
            });
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            bytes = buffer.array();
            log.debug("Calculated block bytes {}, id {}", Convert.toHexString(bytes), AplIdGenerator.BLOCK.getId(bytes));
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
    public void checkFailedTxsExecution() {
        // assuming transactions statuses were obtained by the node block txs execution with own statuses
        List<TxErrorHash> actualErrors = obtainTxErrorHashes(blockTransactions);
        if (!actualErrors.equals(txErrorHashes)) {
            throw new AplBlockTxErrorResultsMismatchException(this, actualErrors);
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
        sb.append(", blockTransactions=[").append(blockTransactions.size());
        sb.append(", txErrorHashes=[").append(txErrorHashes.stream().map(TxErrorHash::toString).collect(Collectors.joining(",")));
        sb.append("], baseTarget=").append(baseTarget);
        sb.append(", nextBlockId=").append(nextBlockId);
        sb.append(", height=").append(height);
        sb.append(", stringId='").append(stringId).append('\'');
        sb.append(", generatorId=").append(generatorId);
        sb.append(", hasValidSignature=").append(hasValidSignature);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean hasLoadedData() {
        return hasLoadedData;
    }

    @Override
    public void assignBlockData(List<Transaction> txs, byte[] generatorPublicKey) {
        this.blockTransactions = Collections.unmodifiableList(txs);
        assignTransactionsIndex();
        this.txErrorHashes = obtainTxErrorHashes(txs);
        this.generatorPublicKey = generatorPublicKey;
        if (generatorPublicKey != null) {
            this.generatorId = Convert.getId(generatorPublicKey);
        }
        this.hasLoadedData = true;
    }

    private void assignTransactionsIndex() {
        // important !!! assign transaction index value
        short index = 0;
        for (Transaction transaction : this.blockTransactions) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    private List<TxErrorHash> obtainTxErrorHashes(List<Transaction> blockTransactions) {
        return blockTransactions.stream()
            .filter(Transaction::isFailed)
            .map(tx -> new TxErrorHash(tx.getId(), tx.getErrorMessage()
                .orElseThrow(() -> new IllegalStateException("Only failed txs should be added to the list of tx hashed errors"))))
            .collect(Collectors.toList());
    }

}
