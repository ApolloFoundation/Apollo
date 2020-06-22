/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public final class CurrencySellOffer extends CurrencyExchangeOffer {
    private static final BlockChainInfoService BLOCK_CHAIN_INFO_SERVICE =
        CDI.current().select(BlockChainInfoService.class).get();

    /**
     * @deprecated
     */
    private static final LongKeyFactory<CurrencySellOffer> sellOfferDbKeyFactory = new LongKeyFactory<CurrencySellOffer>("id") {

        @Override
        public DbKey newKey(CurrencySellOffer sell) {
            return sell.dbKey;
        }

    };

    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<CurrencySellOffer> sellOfferTable = new VersionedDeletableEntityDbTable<CurrencySellOffer>("sell_offer", sellOfferDbKeyFactory) {

        @Override
        public CurrencySellOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencySellOffer(rs, dbKey);
        }

        @Override
        public void save(Connection con, CurrencySellOffer sell) throws SQLException {
            sell.save(con, table);
        }

    };
    private final DbKey dbKey;

    private CurrencySellOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getSellRateATM(),
            attachment.getTotalSellLimit(), attachment.getInitialSellSupply(), attachment.getExpirationHeight(), transaction.getHeight(),
            transaction.getIndex());
        this.dbKey = sellOfferDbKeyFactory.newKey(id);
    }

    private CurrencySellOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.dbKey = dbKey;
    }

    /**
     * @deprecated
     */
    public static int getCount() {
        return sellOfferTable.getCount();
    }

    /**
     * @deprecated
     */
    public static CurrencySellOffer getOffer(long id) {
        return sellOfferTable.get(sellOfferDbKeyFactory.newKey(id));
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getAll(int from, int to) {
        return sellOfferTable.getAll(from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return sellOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return sellOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    /**
     * @deprecated
     */
    public static CurrencySellOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    /**
     * @deprecated
     */
    public static CurrencySellOffer getOffer(final long currencyId, final long accountId) {
        return sellOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId).and(new DbClause.LongClause("account_id", accountId)));
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to) {
        return sellOfferTable.getManyBy(dbClause, from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return sellOfferTable.getManyBy(dbClause, from, to, sort);
    }

    /**
     * @deprecated
     */
    static void addOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        sellOfferTable.insert(new CurrencySellOffer(transaction, attachment));
    }

    /**
     * @deprecated
     */
    static void remove(CurrencySellOffer sellOffer) {
        sellOfferTable.deleteAtHeight(sellOffer, BLOCK_CHAIN_INFO_SERVICE.getHeight());
    }

    /**
     * @deprecated
     */
    public static void init() {
    }

    @Override
    /**
     * @deprecated
     */
    public long getSupply() {
        return super.getSupply();
    }

    /**
     * @deprecated
     */
    @Override
    public long getCurrencyId() {
        return super.getCurrencyId();
    }

    /**
     * @deprecated
     */
    @Override
    public CurrencyBuyOffer getCounterOffer() {
        return CurrencyBuyOffer.getOffer(id);
    }

    /**
     * @deprecated
     */
    long increaseSupply(long delta) {
        long excess = super.increaseSupply(delta);
        sellOfferTable.insert(this);
        return excess;
    }

    /**
     * @deprecated
     */
    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        sellOfferTable.insert(this);
    }
}
