/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPublicKeyService {

    int getCount();

    int getPublicKeysCount();

    int getGenesisPublicKeysCount();

    List<PublicKey> loadPublicKeyList(int from, int to, boolean isGenesis);

    /**
     * Returns the public key from cache, or load from the data base if necessary.
     *
     * @param accountId get entity from the cache for specified account Id
     * @return public key or null
     */
    PublicKey getPublicKey(long accountId);

    /**
     * Load public key for specified height.
     *
     * @param accountId id of account for which new public key should be loaded
     * @param height    block height
     * @return public key or null
     */
    PublicKey getByHeight(long accountId, int height);

    /**
     *
     * @param accountId
     * @param key
     * @return
     */
    boolean setOrVerifyPublicKey(long accountId, byte[] key);

    /**
     * Load public key from the database and compare with the specified one
     * @param key the specified public key
     * @return true if specified public key equals the stored in the database one
     */
    boolean verifyPublicKey(byte[] key);

    boolean setOrVerifyPublicKey(long accountId, byte[] key, int height);

    PublicKey insertNewPublicKey(long accountId);

    PublicKey insertGenesisPublicKey(long accountId);

    byte[] getPublicKeyByteArray(long id);

    EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    void apply(Account account, byte[] key);

    void apply(Account account, byte[] key, boolean isGenesis);

    PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis);

    void cleanUpPublicKeys();

}
