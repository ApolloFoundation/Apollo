package com.apollocurrency.aplwallet.apl.core.transaction;

public interface TransactionTypeFactory {

    TransactionType findTransactionType(byte type, byte subtype);

}