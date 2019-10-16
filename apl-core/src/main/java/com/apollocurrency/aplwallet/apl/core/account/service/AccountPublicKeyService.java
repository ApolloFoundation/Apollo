/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPublicKeyService {

    void clearCache();

    void removeFromCache(DbKey key);

    int getCount();

    boolean isCacheEnabled();

    byte[] getPublicKey(long id);

    PublicKey getPublicKey(DbKey dbKey);

    PublicKey getPublicKey(DbKey dbKey, int height);

    EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    boolean setOrVerify(long accountId, byte[] key);

    void apply(Account account, byte[] key);

    void apply(Account account, byte[] key, boolean isGenesis);

    PublicKey insertNewPublicKey(DbKey dbKey, boolean isGenesis);

}
