/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface TransactionV3 extends Transaction{
    String getChainId();

    /**
     * the number of transactions sent by the sender
     * @return the number of transactions sent by the sender
     */
    BigInteger getNonce();

    BigInteger getAmount();//long:getAmountATM
    BigInteger getFuelPrice();
    BigInteger getFuelLimit();
    long getLongTimestamp();//int getTimestamp

    /**
     * Return RLP encoded transaction
     * @return byte array of the RLP encoded transaction
     */
    byte[] rlpEncodedTx();
}
