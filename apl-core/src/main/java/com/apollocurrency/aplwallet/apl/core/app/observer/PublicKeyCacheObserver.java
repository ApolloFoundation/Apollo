package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class PublicKeyCacheObserver {

    private InMemoryCacheManager cacheManager;
    private Cache<DbKey, PublicKey> publicKeyCache;

    @Inject
    public PublicKeyCacheObserver(InMemoryCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.publicKeyCache = cacheManager.acquireCache(PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
    }


    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        publicKeyCache.invalidateAll();
    }

    public void onDbHotSwapBegin(@Observes DbHotSwapConfig dbHotSwapConfig) {
        publicKeyCache.invalidateAll();
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
}
