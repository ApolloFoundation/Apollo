/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.util.List;

public interface PublicKeyService {

    int getPublicKeysCount();

    int getGenesisPublicKeysCount();

    List<PublicKey> loadPublicKeyList(int from, int to, boolean isGenesis);

    /**
     * Returns the public key from cache, or load from the data base if necessary.
     * @param dbKey the key to get entity from the cache
     * @return  public key or null
     */
    PublicKey getPublicKey(DbKey dbKey);

    /**
     * Load public key from the data base
     * @param dbKey the primary key to load entity from the data base
     * @return public key or null
     */
    PublicKey loadPublicKey(DbKey dbKey);

    /**
     * Load public key for specified height from the data base
     * @param dbKey the primary key to load entity
     * @param height block height
     * @return public key or null
     */
    PublicKey loadPublicKey(DbKey dbKey, int height);

    boolean setOrVerifyPublicKey(DbKey dbKey, byte[] key, int height);

    @Deprecated
    PublicKey newEntity(DbKey dbKey);

    PublicKey insertNewPublicKey(DbKey dbKey);

    PublicKey insertGenesisPublicKey(DbKey dbKey);

    int getCount();

    PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis);

    /**
     * Forget all public keys by cleaning the internal cache.
     */
    void cleanUpPublicKeysInMemory();

}
