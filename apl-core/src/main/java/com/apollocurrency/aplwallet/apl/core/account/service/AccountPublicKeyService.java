/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountPublicKeyService {


    int getCount();

    Map<DbKey, byte[]> getPublicKeyCache();

    byte[] getPublicKey(long id);

    PublicKey getPublicKey(DbKey dbKey);

    PublicKey getPublicKey(DbKey dbKey, int height);

    EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);

    boolean setOrVerify(long accountId, byte[] key);

    void apply(AccountEntity account, byte[] key);

    void apply(AccountEntity account, byte[] key, boolean isGenesis);

    void insertNewPublicKey(DbKey dbKey, boolean isGenesis);

}
