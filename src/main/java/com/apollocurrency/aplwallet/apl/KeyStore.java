package com.apollocurrency.aplwallet.apl;

public interface KeyStore {
    /**
     * Return secret bytes if key exists for accountId and can be decrypted by passphrase
     * @param passphrase - string, which consist of random words for keySeed decryption
     * @param accountId - id of account, which keySeed should be decrypted
     * @return decrypted secret bytes with status OK or null with fail status
     */
    SecretBytesDetails getSecretBytes(String passphrase, long accountId);

    /**
     * Save encrypted by passphrase secretBytes to keystore
     * @param passphrase - string, which consist of random words for encryption
     * @param secretBytes - secret array of bytes which will be stored into keystore
     * @return true - if secretBytes were saved successfully, otherwise - false
     */
    boolean saveSecretBytes(String passphrase, byte[] secretBytes);

}
