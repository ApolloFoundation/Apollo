package com.apollocurrency.aplwallet.apl.core.transaction.messages;

public interface Encryptable {

    void encrypt(byte[] keySeed);

}
