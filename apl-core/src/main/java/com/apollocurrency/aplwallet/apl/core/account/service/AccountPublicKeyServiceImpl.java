/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Singleton
public class AccountPublicKeyServiceImpl implements AccountPublicKeyService {

    //TODO: make cache injectable. There is quite nice cache library with eviction policy. https://github.com/ben-manes/caffeine
    private ConcurrentMap<DbKey, byte[]> publicKeyCache = null;
    private boolean cacheEnabled = false;

    private PropertiesHolder propertiesHolder;
    private Blockchain blockchain;
    private PublicKeyTable publicKeyTable;
    private GenesisPublicKeyTable genesisPublicKeyTable;

    @Inject
    public AccountPublicKeyServiceImpl(PropertiesHolder propertiesHolder, Blockchain blockchain, PublicKeyTable publicKeyTable, GenesisPublicKeyTable genesisPublicKeyTable) {
        this.propertiesHolder = propertiesHolder;
        this.blockchain = blockchain;
        this.publicKeyTable = publicKeyTable;
        this.genesisPublicKeyTable = genesisPublicKeyTable;
    }

    @PostConstruct
    void init(){
        if (propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache")) {
            publicKeyCache = new ConcurrentHashMap<>();
            cacheEnabled = true;
        }
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    @Override
    public Map<DbKey, byte[]> getPublicKeyCache() {
        return publicKeyCache;
    }

    private byte[] putInCache(DbKey key, byte[] value){
        if (isCacheEnabled()){
            return publicKeyCache.put(key, value);
        }else {
            return null;
        }
    }

    private byte[] getFromCache(DbKey key){
        if (isCacheEnabled()){
            return publicKeyCache.get(key);
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
        byte[] key = getFromCache(dbKey);
        if (key == null) {
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey == null || (key = publicKey.getPublicKey()) == null) {
                return null;
            }
            putInCache(dbKey, key);
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
        DbKey dbKey = publicKeyTable.newKey(accountId);
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            publicKey.setHeight(blockchain.getHeight());
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
        } else if (!Arrays.equals(publicKey.getPublicKey(), key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockchain.getHeight() - 1) {
            PublicKey dbPublicKey = getPublicKey(account.getDbKey(), false);
            if (dbPublicKey == null || dbPublicKey.getPublicKey() == null) {
                publicKeyTable.insert(publicKey);
            }
        }
        putInCache(account.getDbKey(), key);
        account.setPublicKey(publicKey);
    }

    @Override
    public PublicKey insertNewPublicKey(DbKey dbKey, boolean isGenesis) {
        EntityDbTable<PublicKey> table = isGenesis ? genesisPublicKeyTable: publicKeyTable;
        PublicKey publicKey = table.newEntity(dbKey);
        table.insert(publicKey);
        return publicKey;
    }
}
