/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class DGSObserver {
    private DGSService service;
    private AccountService accountService;

    @Inject
    public DGSObserver(DGSService service, AccountService accountService) {
        this.service = service;
        this.accountService = accountService;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        if (block.getHeight() == 0) {
            return;
        }
        log.trace(":accept:DGSObserver: START onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
        List<DGSPurchase> expiredPurchases = new ArrayList<>();
        try (DbIterator<DGSPurchase> iterator = service.getExpiredPendingPurchases(block)) {
            while (iterator.hasNext()) {
                expiredPurchases.add(iterator.next());
            }
        }
        for (DGSPurchase purchase : expiredPurchases) {
            Account buyer = accountService.getAccount(purchase.getBuyerId());
            accountService.addToUnconfirmedBalanceATM(buyer, LedgerEvent.DIGITAL_GOODS_PURCHASE_EXPIRED, purchase.getId(),
                    Math.multiplyExact(purchase.getQuantity(), purchase.getPriceATM()));
            DGSGoods goods = service.getGoods(purchase.getGoodsId());
            goods.setHeight(block.getHeight());
            service.changeQuantity(goods, purchase.getQuantity());
            purchase.setHeight(block.getHeight());
            service.setPending(purchase, false);
        }
        log.trace(":accept:DGSObserver: END onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
    }
}
