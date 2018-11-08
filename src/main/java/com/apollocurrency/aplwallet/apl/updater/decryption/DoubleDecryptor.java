package com.apollocurrency.aplwallet.apl.updater.decryption;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

public interface DoubleDecryptor {
    byte[] decrypt(byte[] encryptedBytes, PublicKey pk1, PublicKey pk2) throws GeneralSecurityException;
}
