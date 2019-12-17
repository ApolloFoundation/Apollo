/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountPublicKeyServiceImpl implements AccountPublicKeyService {

    private InMemoryCacheManager cacheManager;
    private Cache<DbKey, PublicKey> publicKeyCache = null;

    private boolean cacheEnabled = false;

    private PropertiesHolder propertiesHolder;
    private Blockchain blockchain;
    private PublicKeyTable publicKeyTable;
    private GenesisPublicKeyTable genesisPublicKeyTable;

    @Inject
    public AccountPublicKeyServiceImpl(PropertiesHolder propertiesHolder, Blockchain blockchain, PublicKeyTable publicKeyTable, GenesisPublicKeyTable genesisPublicKeyTable, InMemoryCacheManager cacheManager) {
        this.propertiesHolder = propertiesHolder;
        this.blockchain = blockchain;
        this.publicKeyTable = publicKeyTable;
        this.genesisPublicKeyTable = genesisPublicKeyTable;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    void init(){
        if (propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache")) {
            publicKeyCache = cacheManager.acquireCache(PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
            cacheEnabled = true;
        }
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    @Override
    public void clearCache() {
        if ( isCacheEnabled()) {
            publicKeyCache.invalidateAll();
        }
    }

    @Override
    public void removeFromCache(DbKey key) {
        if ( isCacheEnabled()) {
            publicKeyCache.invalidate(key);
        }
    }

    private void putInCache(DbKey key, PublicKey value){
        if (isCacheEnabled()){
            publicKeyCache.put(key, value);
        }
    }

    private void updateInCache(DbKey dbKey) {
        if ( isCacheEnabled()) {
            PublicKey key = publicKeyTable.get(dbKey, true);
            if (key != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
    }

    private PublicKey getFromCache(DbKey key){
        if (isCacheEnabled()){
            return publicKeyCache.getIfPresent(key);
        }else{
            return null;
        }
    }

    @Override
    public int getCount(){
        return publicKeyTable.getCount() + genesisPublicKeyTable.getCount();
    }

    @Override
    public byte[] getPublicKey(long id) {
        DbKey dbKey = publicKeyTable.newKey(id);
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null || publicKey.getPublicKey() == null) {
            return null;
        }
        return publicKey.getPublicKey();
    }

    @Override
    public PublicKey getPublicKey(DbKey dbKey) {
        PublicKey publicKey = getFromCache(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.get(dbKey);
            if (publicKey == null) {
                publicKey = genesisPublicKeyTable.get(dbKey);
            }
            if (publicKey != null) {
                putInCache(dbKey, publicKey);
            }
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
        DbKey dbKey = publicKeyTable.newKey(accountId);
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            publicKey.setHeight(blockchain.getHeight());
            putInCache(dbKey, publicKey);
            return true;
        }
        return Arrays.equals(publicKey.getPublicKey(), key);
    }

    @Override
    public void apply(Account account, byte[] key) {
        apply(account, key, false);
    }

    @Override
    public void apply(Account account, byte[] key, boolean isGenesis) {
        PublicKey publicKey = getPublicKey(account.getDbKey());
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(account.getDbKey());
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            if (isGenesis) {
                genesisPublicKeyTable.insert(publicKey);
            } else {
                publicKeyTable.insert(publicKey);
            }
            updateInCache(account.getDbKey());
        } else if (!Arrays.equals(publicKey.getPublicKey(), key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockchain.getHeight() - 1) {
            PublicKey dbPublicKey = getPublicKey(account.getDbKey(), false);
            if (dbPublicKey == null || dbPublicKey.getPublicKey() == null) {
                publicKeyTable.insert(publicKey);
                updateInCache(account.getDbKey());
            }
        } else {
            putInCache(account.getDbKey(), publicKey);
        }
        account.setPublicKey(publicKey);
    }

    @Override
    public PublicKey insertNewPublicKey(DbKey dbKey, boolean isGenesis) {
        EntityDbTable<PublicKey> table = isGenesis ? genesisPublicKeyTable: publicKeyTable;
        PublicKey publicKey = table.newEntity(dbKey);
        table.insert(publicKey);
        if ( isCacheEnabled()) {
            publicKeyCache.put(dbKey, table.get(dbKey, true));
        }
        return publicKey;
    }
}
