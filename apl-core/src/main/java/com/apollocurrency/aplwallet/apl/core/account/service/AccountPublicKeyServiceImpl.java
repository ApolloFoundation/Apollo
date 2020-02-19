/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountPublicKeyServiceImpl implements AccountPublicKeyService {

    private final Blockchain blockchain;
    private final EntityDbTableInterface<PublicKey> publicKeyTable;
    private final EntityDbTableInterface<PublicKey> genesisPublicKeyTable;
    private final InMemoryCacheManager cacheManager;
    @Getter
    private final boolean cacheEnabled;
    @Getter
    private Cache<DbKey, PublicKey> publicKeyCache;

    @Inject
    public AccountPublicKeyServiceImpl(@Named("publicKeyTable") EntityDbTableInterface<PublicKey> publicKeyTable,
                                       @Named("genesisPublicKeyTable") EntityDbTableInterface<PublicKey> genesisPublicKeyTable,
                                       PropertiesHolder propertiesHolder,
                                       Blockchain blockchain,
                                       InMemoryCacheManager cacheManager) {
        this.blockchain = blockchain;
        this.publicKeyTable = publicKeyTable;
        this.genesisPublicKeyTable = genesisPublicKeyTable;
        this.cacheManager = cacheManager;
        this.cacheEnabled = propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache");
    }

    @PostConstruct
    void init(){
        if (isCacheEnabled()){
            publicKeyCache = cacheManager.acquireCache(PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
            log.debug("--cache-- init PUBLIC KEY CACHE={}", publicKeyCache);
        }
    }
    void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        clearCache();
    }

    void onDbHotSwapBegin(@Observes DbHotSwapConfig dbHotSwapConfig) {
        clearCache();
    }

    //TODO: Don't remove this comment, that code might be helpful for further data layer redesign
    /*
    void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        if (isCacheEnabled()) {
            removeFromCache(AccountTable.newKey(block.getGeneratorId()));
            block.getOrLoadTransactions().forEach(transaction -> {
                removeFromCache(AccountTable.newKey(transaction.getSenderId()));
                if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                    removeFromCache(AccountTable.newKey(transaction.getRecipientId()));
                }
                if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                    ShufflingRecipientsAttachment shufflingRecipients = (ShufflingRecipientsAttachment) transaction.getAttachment();
                    for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                        removeFromCache(AccountTable.newKey(Account.getId(publicKey)));
                    }
                }
            });
        }
    }
    */

    @Override
    public int getCount(){
        return getPublicKeysCount() + getGenesisPublicKeysCount();
    }

    @Override
    public int getPublicKeysCount(){
        return publicKeyTable.getCount();
    }

    @Override
    public int getGenesisPublicKeysCount(){
        return genesisPublicKeyTable.getCount();
    }

    @Override
    public byte[] getPublicKeyByteArray(long id) {
        DbKey dbKey = AccountTable.newKey(id);
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

    @Override
    public PublicKey loadPublicKeyFromDb(DbKey dbKey) {
        PublicKey publicKey = publicKeyTable.get(dbKey, false);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, false);
        }
        return publicKey;
    }

    @Override
    public PublicKey loadPublicKeyFromDb(DbKey dbKey, int height) {
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, height);
        }
        return publicKey;
    }

    @Override
    public List<PublicKey> loadPublicKeyList(int from, int to, boolean isGenesis) {
        EntityDbTableInterface<PublicKey> table = isGenesis ? genesisPublicKeyTable : publicKeyTable;
        return toList(table.getAll(from, to));
    }

    @Override
    public EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress) {
        byte[] key = getPublicKeyByteArray(id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return encryptTo(key, data, keySeed, compress);
    }

    @Override
    public byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] key = getPublicKeyByteArray(id);
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
    public boolean setOrVerifyPublicKey(long accountId, byte[] key) {
        DbKey dbKey = AccountTable.newKey(accountId);
        return setOrVerifyPublicKey(dbKey, key, blockchain.getHeight());
    }

    @Override
    public boolean setOrVerifyPublicKey(DbKey dbKey, byte[] key, int height) {
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = new PublicKey(((LongKey) dbKey).getId(), null, blockchain.getHeight());
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            publicKey.setHeight(height);
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
            publicKey = new PublicKey(account.getId(), null, blockchain.getHeight());
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            insertPublicKey(publicKey, isGenesis);
        } else if (!Arrays.equals(publicKey.getPublicKey(), key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockchain.getHeight() - 1) {
            PublicKey dbPublicKey = loadPublicKeyFromDb(account.getDbKey());
            if (dbPublicKey == null || dbPublicKey.getPublicKey() == null) {
                insertPublicKey(publicKey, isGenesis);
            }
        }
        account.setPublicKey(publicKey);
    }

    @Override
    public PublicKey insertNewPublicKey(DbKey dbKey) {
        PublicKey publicKey = new PublicKey(((LongKey) dbKey).getId(), null, blockchain.getHeight());
        publicKeyTable.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertGenesisPublicKey(DbKey dbKey) {
        PublicKey publicKey = new PublicKey(((LongKey) dbKey).getId(), null, blockchain.getHeight());
        genesisPublicKeyTable.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis) {
        if(isGenesis){
            genesisPublicKeyTable.insert(publicKey);
        }else{
            publicKeyTable.insert(publicKey);
        }
        return publicKey;
    }

    public void cleanUpPublicKeysInMemory() {
        clearCache();
    }

    public void cleanUpPublicKeys() {
        publicKeyTable.truncate();
        genesisPublicKeyTable.truncate();
    }

    private boolean isCacheEnabled() {
        return cacheEnabled;
    }

    private void clearCache() {
        if ( isCacheEnabled()) {
            publicKeyCache.invalidateAll();
        }
    }

    private void removeFromCache(DbKey key) {
        if ( isCacheEnabled()) {
            log.trace("--cache-- remove dbKey={}", key);
            publicKeyCache.invalidate(key);
        }
    }

    private PublicKey getFromCache(DbKey key){
        if (isCacheEnabled()){
            PublicKey pkey = publicKeyCache.getIfPresent(key);
            log.trace("--cache-- get dbKey={}, from cache pkey={}", key, pkey);
            return pkey;
        }else{
            return null;
        }
    }

    private void refreshInCache(DbKey dbKey) {
        if ( isCacheEnabled()) {
            PublicKey publicKey = loadPublicKeyFromDb(dbKey);
            if (publicKey != null) {
                log.trace("--cache-- refresh dbKey={} height={}", dbKey, publicKey.getHeight());
                publicKeyCache.put(dbKey, publicKey);
            }
        }
    }

    private void putInCache(DbKey key, PublicKey value){
        if (isCacheEnabled()){
            log.trace("--cache-- put  dbKey={} height={}", key, value.getHeight());
            publicKeyCache.put(key, value);
        }
    }
}
