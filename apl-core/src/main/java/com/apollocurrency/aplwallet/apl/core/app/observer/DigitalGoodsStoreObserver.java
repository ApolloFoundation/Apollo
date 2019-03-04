/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Observes;

public class DigitalGoodsStoreObserver {
    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        if (block.getHeight() == 0) {
            return;
        }
        List<DigitalGoodsStore.Purchase> expiredPurchases = new ArrayList<>();
        try (DbIterator<DigitalGoodsStore.Purchase> iterator = DigitalGoodsStore.Purchase.getExpiredPendingPurchases(block)) {
            while (iterator.hasNext()) {
                expiredPurchases.add(iterator.next());
            }
        }
        for (DigitalGoodsStore.Purchase purchase : expiredPurchases) {
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_PURCHASE_EXPIRED, purchase.getId(),
                    Math.multiplyExact((long) purchase.getQuantity(), purchase.getPriceATM()));
            DigitalGoodsStore.Goods.getGoods(purchase.getGoodsId()).changeQuantity(purchase.getQuantity());
            purchase.setPending(false);
        }
    }
}
