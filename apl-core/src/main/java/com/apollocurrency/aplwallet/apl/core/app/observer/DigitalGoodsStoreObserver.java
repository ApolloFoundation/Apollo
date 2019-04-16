/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;

public class DigitalGoodsStoreObserver {
    private DGSService service;

    public DGSService lookupDGService() {
        if (service == null) {
            service = CDI.current().select(DGSService.class).get();
        }
        return service;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        if (block.getHeight() == 0) {
            return;
        }
        List<DGSPurchase> expiredPurchases = new ArrayList<>();
        try (DbIterator<DGSPurchase> iterator = lookupDGService().getExpiredPendingPurchases(block)) {
            while (iterator.hasNext()) {
                expiredPurchases.add(iterator.next());
            }
        }
        for (DGSPurchase purchase : expiredPurchases) {
            Account buyer = Account.getAccount(purchase.getBuyerId());
            buyer.addToUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_PURCHASE_EXPIRED, purchase.getId(),
                    Math.multiplyExact((long) purchase.getQuantity(), purchase.getPriceATM()));
            DGSGoods goods = service.getGoods(purchase.getGoodsId());
            service.changeQuantity(goods, purchase.getQuantity());
            purchase.setPending(false);
        }
    }
}
