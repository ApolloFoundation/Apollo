package com.apollocurrency.aplwallet.apl;

public interface TwoFactorAuthRepository {
    /**
     * Retrieves secret word for account if exists, if not exists throw exception
     * @param account - id of account, which secret word should be retrieved
     * @return - account secret word byte array
     * @throws NotFoundException if no secret word exist for this account
     */
    byte[] getSecret(long account) throws NotFoundException;

    /**
     * Save new secret word, which will be associated with this account. If
     * this account already has secret word, AlreadyExistsException will be thrown
     * @param account - id of account which should be saved with associated secret word
     * @param secret - secret word bytes
     * @throws AlreadyExistsException if account already has secret word
     */
    void saveSecret(long account, byte[] secret) throws AlreadyExistsException;

    /**
     * Deletes assciated with this account secret word entry, if account does not have secret word throw exception
     * @param account - id of account which secret word entry should be deleted
     * @throws NotFoundException - if no entries found for this account
     */
    void delete(long account) throws NotFoundException;

}
