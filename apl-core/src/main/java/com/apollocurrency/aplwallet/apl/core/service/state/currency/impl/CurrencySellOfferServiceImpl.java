/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import static com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyExchangeOfferFacadeImpl.availableOnlyDbClause;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySellOfferTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencySellOfferService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CurrencySellOfferServiceImpl implements CurrencySellOfferService {

    private final CurrencySellOfferTable currencySellOfferTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<CurrencySellOffer> iteratorToStreamConverter;

    @Inject
    public CurrencySellOfferServiceImpl(CurrencySellOfferTable currencySellOfferTable,
                                        BlockChainInfoService blockChainInfoService) {
        this.currencySellOfferTable = currencySellOfferTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit tests
     */
    public CurrencySellOfferServiceImpl(CurrencySellOfferTable currencySellOfferTable,
                                        BlockChainInfoService blockChainInfoService,
                                        IteratorToStreamConverter<CurrencySellOffer> iteratorToStreamConverter) {
        this.currencySellOfferTable = currencySellOfferTable;
        this.blockChainInfoService = blockChainInfoService;
        if (iteratorToStreamConverter != null) {
            this.iteratorToStreamConverter = iteratorToStreamConverter;
        } else {
            this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public int getCount() {
        return currencySellOfferTable.getCount();
    }

    @Override
    public CurrencySellOffer getOffer(long offerId) {
        return currencySellOfferTable.get(CurrencySellOfferTable.sellOfferDbKeyFactory.newKey(offerId));
    }

    @Override
    public DbIterator<CurrencySellOffer> getAll(int from, int to) {
        return currencySellOfferTable.getAll(from, to);
    }

    @Override
    public Stream<CurrencySellOffer> getAllStream(int from, int to) {
        return iteratorToStreamConverter.apply(currencySellOfferTable.getAll(from, to));
    }

    @Override
    public DbIterator<CurrencySellOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    @Override
    public Stream<CurrencySellOffer> getOffersStream(Currency currency, int from, int to) {
        return getCurrencyOffersStream(currency.getId(), false, from, to);
    }

    @Override
    public DbIterator<CurrencySellOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return currencySellOfferTable.getManyBy(dbClause, from, to,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    @Override
    public Stream<CurrencySellOffer> getCurrencyOffersStream(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return iteratorToStreamConverter.apply(
            currencySellOfferTable.getManyBy(dbClause, from, to,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")
        );
    }

    @Override
    public DbIterator<CurrencySellOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return currencySellOfferTable.getManyBy(dbClause, from, to,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    @Override
    public Stream<CurrencySellOffer> getAccountOffersStream(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return iteratorToStreamConverter.apply(
            currencySellOfferTable.getManyBy(dbClause, from, to,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")
        );
    }

    @Override
    public CurrencySellOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    @Override
    public CurrencySellOffer getOffer(final long currencyId, final long accountId) {
        return currencySellOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId)
            .and(new DbClause.LongClause("account_id", accountId)));
    }

    @Override
    public DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to) {
        return currencySellOfferTable.getManyBy(dbClause, from, to);
    }

    @Override
    public Stream<CurrencySellOffer> getOffersStream(DbClause dbClause, int from, int to) {
        return iteratorToStreamConverter.apply(currencySellOfferTable.getManyBy(dbClause, from, to));
    }

    @Override
    public DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return currencySellOfferTable.getManyBy(dbClause, from, to, sort);
    }

    @Override
    public Stream<CurrencySellOffer> getOffersStream(DbClause dbClause, int from, int to, String sort) {
        return iteratorToStreamConverter.apply(currencySellOfferTable.getManyBy(dbClause, from, to, sort));
    }

    @Override
    public void addOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        CurrencySellOffer currencyBuyOffer = new CurrencySellOffer(transaction, attachment, blockChainInfoService.getHeight());
        currencySellOfferTable.insert(currencyBuyOffer);
    }

    @Override
    public void remove(CurrencySellOffer sellOffer) {
        int height = blockChainInfoService.getHeight();
        sellOffer.setHeight(height); // important to assign height here!
        currencySellOfferTable.deleteAtHeight(sellOffer, height);
    }

    @Override
    public void insert(CurrencySellOffer currencySellOffer) {
        currencySellOffer.setHeight(blockChainInfoService.getHeight());// important to assign height here!
        currencySellOfferTable.insert(currencySellOffer);
    }

}
