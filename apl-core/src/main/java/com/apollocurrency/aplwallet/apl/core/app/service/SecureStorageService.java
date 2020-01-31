package com.apollocurrency.aplwallet.apl.core.app.service;

import com.apollocurrency.aplwallet.apl.util.AplException;

import java.nio.file.Path;
import java.util.List;

public interface SecureStorageService {

    static final String SECURE_STORE_KEY = "secure_store_key";

    /**
     * Add user passPhrase to the storage.
     */
    void addUserPassPhrase(Long accountId, String passPhrase);

    /**
     * Get user passPhrase from the storage.
     */
    String getUserPassPhrase(Long accountId);

    /**
     * Get list of users in the storage.
     */
    List<Long> getAccounts();

    /**
     * Save encrypted by passphrase secretStore in the json format.
     * @return true - if secretStore were saved successfully, otherwise returned false
     */
    boolean storeSecretStorage();

    /**
     * Restore user keys from secure storage.
     */
    boolean restoreSecretStorage(Path path);

    /**
     * Delete secure storage file.
     */
    boolean deleteSecretStorage(Path path);

    /**
     * Create private key for application.
     */
    String createPrivateKeyForStorage() throws AplException.ExecutiveProcessException;

    /**
     * Flushing keys after decentralized exchange routine
     * @param accountID   id of the corresponding account
     * @param passPhrase  passphrase of the particular wallet
     * @return flag whether the corresponding pair was found
     */
    boolean flushAccountKeys(Long accountID, String passPhrase);

    boolean isEnabled();
}
