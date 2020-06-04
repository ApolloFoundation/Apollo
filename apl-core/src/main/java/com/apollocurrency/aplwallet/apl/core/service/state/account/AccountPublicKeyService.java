/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
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
     * Returns the public key from cache, or load from the data base if necessary.
     *
     * @param dbKey the key to getting an entity from the cache
     * @return public key or null
     */
    PublicKey getPublicKey(DbKey dbKey);

    /**
     * Load public key from the data base
     *
     * @param dbKey the primary key to load entity from the data base
     * @return public key or null
     */
    PublicKey loadPublicKeyFromDb(DbKey dbKey);

    /**
     * Load public key for specified height from the data base
     *
     * @param dbKey  the primary key to load entity
     * @param height block height
     * @return public key or null
     */
    PublicKey loadPublicKeyFromDb(DbKey dbKey, int height);

    boolean setOrVerifyPublicKey(long accountId, byte[] key);

    boolean setOrVerifyPublicKey(DbKey dbKey, byte[] key, int height);

    PublicKey insertNewPublicKey(DbKey dbKey);

    PublicKey insertGenesisPublicKey(DbKey dbKey);

    byte[] getPublicKeyByteArray(long id);

    EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    void apply(Account account, byte[] key);

    void apply(Account account, byte[] key, boolean isGenesis);

    PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis);

    /**
     * Forget all public keys by cleaning the internal cache.
     */
    void cleanUpPublicKeysInMemory();

    void cleanUpPublicKeys();

}
