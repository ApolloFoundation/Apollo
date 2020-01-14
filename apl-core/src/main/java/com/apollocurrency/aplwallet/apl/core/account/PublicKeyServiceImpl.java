/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;

@Slf4j
@Singleton
public class PublicKeyServiceImpl implements PublicKeyService {

    @Getter
    private final boolean cacheEnabled;
    @Getter
    private final Cache<DbKey, PublicKey> publicKeyCache;

    private EntityDbTableInterface<PublicKey> publicKeyTable;
    private EntityDbTableInterface<PublicKey> genesisPublicKeyTable;

    @Inject
    public PublicKeyServiceImpl(@Named("publicKeyTable") EntityDbTableInterface<PublicKey> publicKeyTable,
                                @Named("genesisPublicKeyTable") EntityDbTableInterface<PublicKey> genesisPublicKeyTable,
                                @Property("apl.enablePublicKeyCache") boolean cacheEnabled,
                                @CacheProducer @CacheType("PUBLIC_KEY_CACHE") Cache<DbKey, PublicKey> publicKeyCache ) {
        this.publicKeyTable = publicKeyTable;
        this.genesisPublicKeyTable = genesisPublicKeyTable;
        this.cacheEnabled = cacheEnabled;
        this.publicKeyCache = publicKeyCache;

        log.debug("--cache-- init PUBLIC KEY CACHE={}", publicKeyCache);
    }

    void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        clearCache();
    }

    void onDbHotSwapBegin(@Observes DbHotSwapConfig dbHotSwapConfig) {
        clearCache();
    }

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

    @Override
    public int getCount(){
        return publicKeyTable.getCount() + genesisPublicKeyTable.getCount();
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
    public PublicKey loadPublicKey(DbKey dbKey) {
        PublicKey publicKey = publicKeyTable.get(dbKey, false);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, false);
        }
        return publicKey;
    }

    @Override
    public PublicKey loadPublicKey(DbKey dbKey, int height) {
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, height);
        }
        return publicKey;
    }

    @Override
    public boolean setOrVerifyPublicKey(DbKey dbKey, byte[] key, int height) {
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKey.setHeight(height);
            putInCache(dbKey, publicKey);
            return true;
        }
        return Arrays.equals(publicKey.publicKey, key);
    }

    @Override
    public PublicKey newEntity(DbKey dbKey){
        return publicKeyTable.newEntity(dbKey);
    }

    @Override
    public PublicKey insertNewPublicKey(DbKey dbKey) {
        PublicKey publicKey = publicKeyTable.newEntity(dbKey);
        publicKeyTable.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertGenesisPublicKey(DbKey dbKey) {
        PublicKey publicKey = genesisPublicKeyTable.newEntity(dbKey);
        genesisPublicKeyTable.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis){
        if(isGenesis){
            genesisPublicKeyTable.insert(publicKey);
        }else{
            publicKeyTable.insert(publicKey);
        }
        return publicKey;
    }

    @Override
    public void cleanUpPublicKeysInMemory() {
        clearCache();
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
            PublicKey key = loadPublicKey(dbKey);
            if (key != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
    }

    private void putInCache(DbKey key, PublicKey value){
        if (isCacheEnabled()){
            publicKeyCache.put(key, value);
        }
    }

}
