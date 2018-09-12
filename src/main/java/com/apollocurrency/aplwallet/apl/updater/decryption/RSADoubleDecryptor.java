/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.decryption;

import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;

public class RSADoubleDecryptor implements DoubleDecryptor {
    @Override
    public byte[] decrypt(byte[] encryptedBytes, PublicKey pk1, PublicKey pk2) throws GeneralSecurityException {
        return RSAUtil.doubleDecrypt(pk1, pk2, new DoubleByteArrayTuple(Arrays.copyOf(encryptedBytes, encryptedBytes.length/2),
                Arrays.copyOfRange(encryptedBytes, encryptedBytes.length/2 + 1, encryptedBytes.length)));
    }
}
