/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import java.math.BigInteger;
import java.util.List;

public interface Block {
    int LEGACY_BLOCK_VERSION = 3;
    int REGULAR_BLOCK_VERSION = 4;
    int INSTANT_BLOCK_VERSION = 5;
    int ADAPTIVE_BLOCK_VERSION = 6;

    int getVersion();

    long getId();

    String getStringId();

    int getHeight();

    void setHeight(int height);

    int getTimestamp();

    long getGeneratorId();

    /**
     * @return hashed failed tx statuses if any
     */
    List<TxErrorHash> getTxErrorHashes();

    byte[] getGeneratorPublicKey();

    boolean hasGeneratorPublicKey();

    long getPreviousBlockId();

    byte[] getPreviousBlockHash();

    long getNextBlockId();

    void setNextBlockId(long nextBlockId);

    long getTotalAmountATM();

    long getTotalFeeATM();

    int getPayloadLength();

    byte[] getPayloadHash();

    List<Transaction> getTransactions();

    byte[] getGenerationSignature();

    byte[] getBlockSignature();

    long getBaseTarget();

    void setBaseTarget(long baseTarget);

    void setCumulativeDifficulty(BigInteger cumulativeDifficulty);

    BigInteger getCumulativeDifficulty();

    byte[] getBytes();

    boolean checkSignature();

    /**
     * Verify that declared failed txs with their statuses are the same as obtained by the node transaction
     * execution. Assuming that transaction execution status was not inherited from the declared block data
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplBlockTxErrorResultsMismatchException when
     * declared tx execution status is not equal to the obtained result
     */
    void checkFailedTxsExecution();

    boolean hasLoadedData();

    int getTimeout();

    void assignBlockData(List<Transaction> txs, byte[] generatorPublicKey);

}
