/*
 * Copyright © 2018-2020 Apollo Foundation
*/

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class CurrencyExchangeOfferObserver {

    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    private final CurrencyBuyOfferService currencyBuyOfferService;

    @Inject
    public CurrencyExchangeOfferObserver(CurrencyExchangeOfferFacade currencyExchangeOfferFacade,
                                         CurrencyBuyOfferService currencyBuyOfferService) {
        this.currencyExchangeOfferFacade = currencyExchangeOfferFacade;
        this.currencyBuyOfferService = currencyBuyOfferService;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:CurrencyExchangeOfferObserver: START onBlockApplied AFTER_BLOCK_APPLY. block={}", block.getHeight());
        List<CurrencyBuyOffer> expired = new ArrayList<>();
        try (DbIterator<CurrencyBuyOffer> offers = currencyBuyOfferService.getOffers(
            new DbClause.IntClause("expiration_height", block.getHeight()), 0, -1)) {
            for (CurrencyBuyOffer offer : offers) {
                expired.add(offer);
            }
        }
        log.trace(":accept:CurrencyExchangeOfferObserver: onBlockApplied AFTER_BLOCK_APPLY. Remove=[{}] offer(s) at height={}",
            expired.size(), block.getHeight());
        expired.forEach((offer) -> currencyExchangeOfferFacade.removeOffer(LedgerEvent.CURRENCY_OFFER_EXPIRED, offer));
        log.trace(":accept:CurrencyExchangeOfferObserver: END onBlockApplied AFTER_BLOCK_APPLY. block={}", block.getHeight());
    }

}
