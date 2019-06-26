/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountPublickKeyServiceImpl implements AccountPublickKeyService {

    private static ConcurrentMap<DbKey, byte[]> publicKeyCache = null;

    @Inject @Setter
    private PropertiesHolder propertiesHolder;

    @Inject @Setter
    private Blockchain blockchain;

    @Inject @Setter
    private PublicKeyTable publicKeyTable;

    @Inject @Setter
    private GenesisPublicKeyTable genesisPublicKeyTable;

    @PostConstruct
    void init(){
        if (propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache")) {
            publicKeyCache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public int getCount(){
        return publicKeyTable.getCount() + genesisPublicKeyTable.getCount();
    }

    @Override
    public Map<DbKey, byte[]> getPublicKeyCache() {
        return publicKeyCache;
    }

    @Override
    public byte[] getPublicKey(long id) {
        DbKey dbKey = PublicKeyTable.newKey(id);
        byte[] key = null;
        if (publicKeyCache != null) {
            key = publicKeyCache.get(dbKey);
        }
        if (key == null) {
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey == null || (key = publicKey.publicKey) == null) {
                return null;
            }
            if (publicKeyCache != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
        return key;
    }

    @Override
    public PublicKey getPublicKey(DbKey dbKey) {
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey);
        }
        return publicKey;
    }

    private PublicKey getPublicKey(DbKey dbKey, boolean cache) {
        PublicKey publicKey = publicKeyTable.get(dbKey, cache);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, cache);
        }
        return publicKey;
    }

    @Override
    public PublicKey getPublicKey(DbKey dbKey, int height) {
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, height);
        }
        return publicKey;
    }

    @Override
    public EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress) {
        byte[] key = getPublicKey(id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return encryptTo(key, data, keySeed, compress);
    }

    @Override
    public byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] key = getPublicKey(id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return decryptFrom(key, encryptedData, recipientKeySeed, uncompress);
    }

    @Override
    public EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, keySeed, publicKey);
    }

    @Override
    public byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientKeySeed, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
    }

    @Override
    public boolean setOrVerify(long accountId, byte[] key) {
        DbKey dbKey = PublicKeyTable.newKey(accountId);
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKey.setHeight(blockchain.getHeight());
            return true;
        }
        return Arrays.equals(publicKey.publicKey, key);
    }

    @Override
    public void apply(AccountEntity account, byte[] key) {
        apply(account, key, false);
    }

    @Override
    public void apply(AccountEntity account, byte[] key, boolean isGenesis) {
        PublicKey publicKey = getPublicKey(account.getDbKey());
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(account.getDbKey());
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            if (isGenesis) {
                GenesisPublicKeyTable.getInstance().insert(publicKey);
            } else {
                publicKeyTable.insert(publicKey);
            }
        } else if (!Arrays.equals(publicKey.publicKey, key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockchain.getHeight() - 1) {
            PublicKey dbPublicKey = getPublicKey(account.getDbKey(), false);
            if (dbPublicKey == null || dbPublicKey.publicKey == null) {
                publicKeyTable.insert(publicKey);
            }
        }
        if (publicKeyCache != null) {
            publicKeyCache.put(account.getDbKey(), key);
        }
        account.setPublicKey(publicKey);
    }

    @Override
    public void insertNewPublicKey(DbKey dbKey, boolean isGenesis) {
        PublicKey publicKey;
        if (isGenesis) {
                publicKey = genesisPublicKeyTable.newEntity(dbKey);
                genesisPublicKeyTable.insert(publicKey);
        } else {
                publicKey = publicKeyTable.newEntity(dbKey);
                publicKeyTable.insert(publicKey);
        }
    }
}
