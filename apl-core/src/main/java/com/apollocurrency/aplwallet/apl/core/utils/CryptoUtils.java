/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class CryptoUtils {
    private CryptoUtils() {
    }

    public static EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, keySeed, publicKey);
    }

    public static byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientKeySeed, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
    }
}
