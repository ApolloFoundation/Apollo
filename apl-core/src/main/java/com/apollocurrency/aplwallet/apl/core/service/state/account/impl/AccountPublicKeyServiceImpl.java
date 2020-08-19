/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountPublicKeyServiceImpl implements AccountPublicKeyService {

    private final InMemoryCacheManager cacheManager;
    @Getter
    private final boolean cacheEnabled;
    private final BlockChainInfoService blockChainInfoService;
    @Getter
    private Cache<Long, PublicKey> publicKeyCache;
    private final PublicKeyDao publicKeyDao;

    @Inject
    public AccountPublicKeyServiceImpl(
        PropertiesHolder propertiesHolder,
        InMemoryCacheManager cacheManager,
        BlockChainInfoService blockChainInfoService, PublicKeyDao publicKeyDao) {
        this.publicKeyDao = publicKeyDao;
        this.cacheManager = cacheManager;
        this.cacheEnabled = propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache");
        this.blockChainInfoService = blockChainInfoService;
    }

    @PostConstruct
    void init() {
        if (isCacheEnabled()) {
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
    public int getCount() {
        return getPublicKeysCount() + getGenesisPublicKeysCount();
    }

    @Override
    public int getPublicKeysCount() {
        return publicKeyDao.count();
    }

    @Override
    public int getGenesisPublicKeysCount() {
        return publicKeyDao.genesisCount();
    }

    @Override
    public byte[] getPublicKeyByteArray(long id) {
        PublicKey publicKey = getPublicKey(id);
        if (publicKey == null || publicKey.getPublicKey() == null) {
            return null;
        }
        return publicKey.getPublicKey();
    }

    @Override
    public PublicKey getPublicKey(long accountId) {
        PublicKey publicKey = getFromCache(accountId);
        if (publicKey == null) {
            publicKey = publicKeyDao.searchAll(accountId);
            if (publicKey != null) {
                putInCache(accountId, publicKey);
            }
        }
        return publicKey;
    }

    @Override
    public PublicKey loadPublicKeyFromDb(long accountId) {
        return publicKeyDao.searchAll(accountId);
    }

    @Override
    public PublicKey loadPublicKeyFromDb(long id, int height) {
        PublicKey publicKey = getPublicKey(id, height);
        if (publicKey == null) {
            publicKey = getGenesisPublicKey(id, height);
        }
        return publicKey;
    }

    /**
     * Gets GenesisPublicKey without checking doesNotExceed and checkAvailable
     * because GenesisPublicKeys are unchangeable.
     *
     * @param id
     * @param height
     * @return
     */
    private PublicKey getGenesisPublicKey(long id, int height) {
        return publicKeyDao.getByHeight(id, height);
    }

    private PublicKey getPublicKey(long id, int height) {
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return publicKeyDao.get(id);
        }
        blockChainInfoService.checkAvailable(height, true);
        return publicKeyDao.getByHeight(id, height);
    }

    @Override
    public List<PublicKey> loadPublicKeyList(int from, int to, boolean isGenesis) {
        if (isGenesis) {
            return publicKeyDao.getAllGenesis(from, to);
        } else {
            return publicKeyDao.getAll(from, to);
        }
    }

    @Override
    public EncryptedData encryptTo(long id, byte[] data, byte[] keySeed, boolean compress) {
        byte[] key = getPublicKeyByteArray(id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return EncryptedDataUtil.encryptTo(key, data, keySeed, compress);
    }

    @Override
    public byte[] decryptFrom(long id, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] key = getPublicKeyByteArray(id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return EncryptedDataUtil.decryptFrom(key, encryptedData, recipientKeySeed, uncompress);
    }

    @Override
    public boolean setOrVerifyPublicKey(long accountId, byte[] key) {
        return setOrVerifyPublicKey(accountId, key, blockChainInfoService.getHeight());
    }

    @Override
    public boolean setOrVerifyPublicKey(long id, byte[] key, int height) {
        PublicKey publicKey = getPublicKey(id);
        if (publicKey == null) {
            publicKey = new PublicKey(id, null, blockChainInfoService.getHeight());
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            publicKey.setHeight(height);
            putInCache(id, publicKey);
            return true;
        }
        return Arrays.equals(publicKey.getPublicKey(), key);
    }

    @Override
    public boolean verifyPublicKey(byte[] key) {
        PublicKey publicKey = getPublicKey(AccountService.getId(key));
        if(publicKey == null || publicKey.getPublicKey() == null){
            return false;
        }
        return Arrays.equals(publicKey.getPublicKey(), key);
    }

    @Override
    public void apply(Account account, byte[] key) {
        apply(account, key, false);
    }

    @Override
    public void apply(Account account, byte[] key, boolean isGenesis) {
        PublicKey publicKey = getPublicKey(account.getId());
        if (publicKey == null) {
            publicKey = new PublicKey(account.getId(), null, blockChainInfoService.getHeight());
        }
        if (publicKey.getPublicKey() == null) {
            publicKey.setPublicKey(key);
            insertPublicKey(publicKey, isGenesis);
        } else if (!Arrays.equals(publicKey.getPublicKey(), key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.getHeight() >= blockChainInfoService.getHeight() - 1) {
            PublicKey dbPublicKey = loadPublicKeyFromDb(account.getId());
            if (dbPublicKey == null || dbPublicKey.getPublicKey() == null) {
                insertPublicKey(publicKey, isGenesis);
            }
        }
        account.setPublicKey(publicKey);
    }

    @Override
    public PublicKey insertNewPublicKey(long accountId) {
        PublicKey publicKey = new PublicKey(accountId, null, blockChainInfoService.getHeight());
        publicKeyDao.insert(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertGenesisPublicKey(long accountId) {
        PublicKey publicKey = new PublicKey(accountId, null, blockChainInfoService.getHeight());
        publicKeyDao.insertGenesis(publicKey);
        return publicKey;
    }

    @Override
    public PublicKey insertPublicKey(PublicKey publicKey, boolean isGenesis) {
        if (isGenesis) {
            publicKeyDao.insertGenesis(publicKey);
        } else {
            publicKeyDao.insert(publicKey);
        }
        return publicKey;
    }

    public void cleanUpPublicKeysInMemory() {
        clearCache();
    }

    public void cleanUpPublicKeys() {
        publicKeyDao.truncate();
    }

    private boolean isCacheEnabled() {
//        return cacheEnabled;
        return false; // temporary IGNORE that cache without considering config property value
    }

    private void clearCache() {
        if (isCacheEnabled()) {
            publicKeyCache.invalidateAll();
        }
    }

    private void removeFromCache(DbKey key) {
        if (isCacheEnabled()) {
            log.trace("--cache-- remove dbKey={}", key);
            publicKeyCache.invalidate(key);
        }
    }

    private PublicKey getFromCache(long accountId) {
        if (isCacheEnabled()) {
            PublicKey pkey = publicKeyCache.getIfPresent(accountId);
            log.trace("--cache-- get dbKey={}, from cache pkey={}", accountId, pkey);
            return pkey;
        } else {
            return null;
        }
    }

    private void refreshInCache(long id) {
        if (isCacheEnabled()) {
            PublicKey publicKey = loadPublicKeyFromDb(id);
            if (publicKey != null) {
                log.trace("--cache-- refresh dbKey={} height={}", id, publicKey.getHeight());
                publicKeyCache.put(id, publicKey);
            }
        }
    }

    private void putInCache(long id, PublicKey value) {
        if (isCacheEnabled()) {
            log.trace("--cache-- put  dbKey={} height={}", id, value.getHeight());
            publicKeyCache.put(id, value);
        }
    }
}
