/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.decryption;

import com.apollocurrency.aplwallet.apl.updater.UpdaterUtil;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

public class RSADoubleDecryptor implements DoubleDecryptor {
    @Override
    public byte[] decrypt(byte[] encryptedBytes, PublicKey pk1, PublicKey pk2) throws GeneralSecurityException {
        return RSAUtil.doubleDecrypt(pk1, pk2, UpdaterUtil.split(encryptedBytes));
    }
}
