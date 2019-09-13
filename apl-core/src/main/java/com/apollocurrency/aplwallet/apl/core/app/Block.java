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

import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.List;

public interface Block {
    int LEGACY_BLOCK_VERSION   = 3;
    int REGULAR_BLOCK_VERSION  = 4;
    int INSTANT_BLOCK_VERSION  = 5;
    int ADAPTIVE_BLOCK_VERSION = 6;

    int getVersion();

    long getId();

    String getStringId();

    int getHeight();

    int getTimestamp();

    long getGeneratorId();

    byte[] getGeneratorPublicKey();

    long getPreviousBlockId();

    byte[] getPreviousBlockHash();

    long getNextBlockId();

    void setNextBlockId(long nextBlockId);

    long getTotalAmountATM();

    long getTotalFeeATM();

    int getPayloadLength();

    byte[] getPayloadHash();

    List<Transaction> getOrLoadTransactions();

    List<Transaction> getTransactions();

    void setTransactions(List<Transaction> transactions);

    byte[] getGenerationSignature();

    byte[] getBlockSignature();

    long getBaseTarget();

    BigInteger getCumulativeDifficulty();

    byte[] getBytes();

    boolean verifyBlockSignature();

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException;

    void setPrevious(Block block);

    JSONObject getJSONObject();

    int getTimeout();

    default String toJsonString() {
        return getJSONObject().toJSONString();
    }
}
