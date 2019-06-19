package com.apollocurrency.aplwallet.apl.core.app.service;

public interface SecureStorageService {

    /**
     * Add user passPhrase to the storage.
     */
    void addUserPassPhrase(Long accountId, String passPhrase);

    /**
     * Get user passPhrase from the storage.
     */
    String getUserPassPhrase(Long accountId);

    /**
     * Save encrypted by passphrase secretStore in the json format.
     * @return true - if secretStore were saved successfully, otherwise returned false
     */
    boolean storeSecretStorage();

    /**
     * Restore user keys from secure storage.
     */
    boolean restoreSecretStorage();

    /**
     * Delete secure storage file.
     */
    boolean deleteSecretStorage();

    /**
     * Create private key for application.
     */
    String createPrivateKeyForStorage();
}
