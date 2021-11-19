/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;

import java.util.List;

public interface CurrencyExchangeOfferFacade {

    void publishOffer(Transaction transaction, MSPublishExchangeOfferAttachment attachment);

    void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer);

    AvailableOffers calculateTotal(List<CurrencyExchangeOffer> offers, long units);

    AvailableOffers getAvailableToBuy(final long currencyId, final long units);

    List<CurrencyExchangeOffer> getAvailableSellOffers(long currencyId, long maxRateATM);

    AvailableOffers getAvailableToSell(long currencyId, long units);

    List<CurrencyExchangeOffer> getAvailableBuyOffers(long currencyId, long minRateATM);

    void exchangeCurrencyForAPL(Transaction transaction, Account account, long currencyId, long rateATM, long units);

    void exchangeAPLForCurrency(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units);

    CurrencyBuyOfferService getCurrencyBuyOfferService();

    CurrencySellOfferService getCurrencySellOfferService();

}
