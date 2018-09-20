package com.apollocurrency.aplwallet.apl;

import java.io.IOException;

public interface KeyStore {
    /**
     * Return key seed bytes if private key exists for accountId and can be decrypted by passphrase
     * @param passphrase - string, which consist of random words for keySeed decryption
     * @param accountId - id of account, which keySeed should be decrypted
     * @return bytes of encrypted keySeed
     * @throws IOException when unable to read or load encrypted keySeed
     * @throws SecurityException when passphrase is incorrect
     */
    byte[] getKeySeed(String passphrase, long accountId) throws IOException, SecurityException;

    /**
     * Save encrypted by passphrase keySeed to keystore
     * @param passphrase - string, which consist of random words for keySeed encryption
     * @param keySeed - secret array of bytes which will be stored into keystore
     * @throws IOException when unable to store encrypted keySeed
     */
    void saveKeySeed(String passphrase, byte[] keySeed) throws IOException;

}
