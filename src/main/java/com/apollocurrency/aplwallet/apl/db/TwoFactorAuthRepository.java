package com.apollocurrency.aplwallet.apl.db;

public interface TwoFactorAuthRepository {
    /**
     * Retrieves 2fa data by id of account
     * @param account id of account, which 2fa entry should be retrieved
     * @return 2fa entry of this account or null if not exist
     *
     */
    TwoFactorAuthEntity get(long account);

    /**
     * Save new 2fa entry
     * @param entity2FA object, which should contain 2fa data for saving
     * @return true if entity2FA was saved successfully, false in other cases
     *
     */
    boolean add(TwoFactorAuthEntity entity2FA);

    /**
     * Update existing 2fa entry
     * @param entity2FA object, which should contain 2fa data for saving
     * @return true if entity2FA was updated successfully, false in other cases
     *
     */
    boolean update(TwoFactorAuthEntity entity2FA);

    /**
     * Deletes associated with this account 2fa entry
     * @param account id of account which 2fa entry should be deleted
     * @return true if at least one entry was deleted, otherwise - false
     */
    boolean delete(long account);

}
