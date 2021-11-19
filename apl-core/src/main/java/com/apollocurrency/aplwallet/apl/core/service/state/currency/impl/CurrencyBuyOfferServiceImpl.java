/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyBuyOfferService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

import static com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyExchangeOfferFacadeImpl.availableOnlyDbClause;

@Slf4j
@Singleton
public class CurrencyBuyOfferServiceImpl implements CurrencyBuyOfferService {

    private final CurrencyBuyOfferTable currencyBuyOfferTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<CurrencyBuyOffer> iteratorToStreamConverter;

    @Inject
    public CurrencyBuyOfferServiceImpl(CurrencyBuyOfferTable currencyBuyOfferTable,
                                       BlockChainInfoService blockChainInfoService) {
        this.currencyBuyOfferTable = currencyBuyOfferTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * for unit tests
     */
    public CurrencyBuyOfferServiceImpl(CurrencyBuyOfferTable currencyBuyOfferTable,
                                       BlockChainInfoService blockChainInfoService,
                                       IteratorToStreamConverter<CurrencyBuyOffer> iteratorToStreamConverter) {
        this.currencyBuyOfferTable = currencyBuyOfferTable;
        this.blockChainInfoService = blockChainInfoService;
        if (iteratorToStreamConverter != null) {
            this.iteratorToStreamConverter = iteratorToStreamConverter;
        } else {
            this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public int getCount() {
        return currencyBuyOfferTable.getCount();
    }

    @Override
    public CurrencyBuyOffer getOffer(long offerId) {
        return currencyBuyOfferTable.get(CurrencyBuyOfferTable.buyOfferDbKeyFactory.newKey(offerId));
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getAll(int from, int to) {
        return currencyBuyOfferTable.getAll(from, to);
    }

    @Override
    public Stream<CurrencyBuyOffer> getAllStream(int from, int to) {
        return iteratorToStreamConverter.apply(currencyBuyOfferTable.getAll(from, to));
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    @Override
    public Stream<CurrencyBuyOffer> getOffersStream(Currency currency, int from, int to) {
        return getCurrencyOffersStream(currency.getId(), false, from, to);
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return currencyBuyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    @Override
    public Stream<CurrencyBuyOffer> getCurrencyOffersStream(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return iteratorToStreamConverter.apply(
            currencyBuyOfferTable.getManyBy(dbClause, from, to,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")
        );
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return currencyBuyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    @Override
    public Stream<CurrencyBuyOffer> getAccountOffersStream(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return iteratorToStreamConverter.apply(
            currencyBuyOfferTable.getManyBy(dbClause, from, to,
                " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")
        );
    }

    @Override
    public CurrencyBuyOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    @Override
    public CurrencyBuyOffer getOffer(final long currencyId, final long accountId) {
        return currencyBuyOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId)
            .and(new DbClause.LongClause("account_id", accountId)));
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to) {
        return currencyBuyOfferTable.getManyBy(dbClause, from, to);
    }

    @Override
    public Stream<CurrencyBuyOffer> getOffersStream(DbClause dbClause, int from, int to) {
        return iteratorToStreamConverter.apply(currencyBuyOfferTable.getManyBy(dbClause, from, to));
    }

    @Override
    public DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return currencyBuyOfferTable.getManyBy(dbClause, from, to, sort);
    }

    @Override
    public Stream<CurrencyBuyOffer> getOffersStream(DbClause dbClause, int from, int to, String sort) {
        return iteratorToStreamConverter.apply(currencyBuyOfferTable.getManyBy(dbClause, from, to, sort));
    }

    @Override
    public void addOffer(Transaction transaction, MSPublishExchangeOfferAttachment attachment) {
        CurrencyBuyOffer currencyBuyOffer = new CurrencyBuyOffer(transaction, attachment, blockChainInfoService.getHeight());
        currencyBuyOfferTable.insert(currencyBuyOffer);
    }

    @Override
    public void remove(CurrencyBuyOffer buyOffer) {
        int height = blockChainInfoService.getHeight();
        buyOffer.setHeight(height); // important to assign height here!
        currencyBuyOfferTable.deleteAtHeight(buyOffer, height);
    }

    @Override
    public void insert(CurrencyBuyOffer currencyBuyOffer) {
        currencyBuyOffer.setHeight(blockChainInfoService.getHeight());// important to assign height here!
        currencyBuyOfferTable.insert(currencyBuyOffer);
    }
}
