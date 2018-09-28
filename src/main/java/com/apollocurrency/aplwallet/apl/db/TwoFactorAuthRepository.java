package com.apollocurrency.aplwallet.apl.db;

public interface TwoFactorAuthRepository {
    /**
     * Retrieves secret word for account if exists
     * @param account id of account, which secret word should be retrieved
     * @return account secret word byte array or null if secret word for this account is not exist
     *
     */
    byte[] getSecret(long account);

    /**
     * Save new secret word, which will be associated with this account.
     * @param account id of account which should be saved with associated secret word
     * @param secret secret word bytes
     * @return true if new secret word was saved successfully, false if secret word for account already exists in db
     *
     */
    boolean saveSecret(long account, byte[] secret);

    /**
     * Deletes associated with this account secret word entry
     * @param account id of account which secret word entry should be deleted
     * @return true if at least one entry was deleted, otherwise - false
     */
    boolean delete(long account);

}
