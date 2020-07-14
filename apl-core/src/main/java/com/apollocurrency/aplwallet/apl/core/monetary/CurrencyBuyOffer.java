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

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public final class CurrencyBuyOffer extends CurrencyExchangeOffer {
    private static final BlockChainInfoService BLOCK_CHAIN_INFO_SERVICE =
        CDI.current().select(BlockChainInfoService.class).get();

    /**
     * @deprecated
     */
    private static final LongKeyFactory<CurrencyBuyOffer> buyOfferDbKeyFactory = new LongKeyFactory<CurrencyBuyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyBuyOffer offer) {
            return offer.dbKey;
        }

    };

    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<CurrencyBuyOffer> buyOfferTable = new VersionedDeletableEntityDbTable<CurrencyBuyOffer>("buy_offer", buyOfferDbKeyFactory) {

        @Override
        public CurrencyBuyOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencyBuyOffer(rs, dbKey);
        }

        @Override
        public void save(Connection con, CurrencyBuyOffer buy) throws SQLException {
            buy.save(con, table);
        }

    };
    private final DbKey dbKey;

    private CurrencyBuyOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateATM(),
            attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight(),
            transaction.getIndex());
        this.dbKey = buyOfferDbKeyFactory.newKey(id);
    }

    private CurrencyBuyOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.dbKey = dbKey;
    }

    /**
     * @deprecated
     */
    public static int getCount() {
        return buyOfferTable.getCount();
    }

    /**
     * @deprecated
     */
    public static CurrencyBuyOffer getOffer(long offerId) {
        return buyOfferTable.get(buyOfferDbKeyFactory.newKey(offerId));
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getAll(int from, int to) {
        return buyOfferTable.getAll(from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return buyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return buyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    /**
     * @deprecated
     */
    public static CurrencyBuyOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    /**
     * @deprecated
     */
    public static CurrencyBuyOffer getOffer(final long currencyId, final long accountId) {
        return buyOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId).and(new DbClause.LongClause("account_id", accountId)));
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to) {
        return buyOfferTable.getManyBy(dbClause, from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return buyOfferTable.getManyBy(dbClause, from, to, sort);
    }

    /**
     * @deprecated
     */
    static void addOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        buyOfferTable.insert(new CurrencyBuyOffer(transaction, attachment));
    }

    /**
     * @deprecated
     */
    static void remove(CurrencyBuyOffer buyOffer) {
        buyOfferTable.deleteAtHeight(buyOffer, BLOCK_CHAIN_INFO_SERVICE.getHeight());
    }

    /**
     * @deprecated
     */
    public static void init() {
    }

    /**
     * @deprecated
     */
    @Override
    public CurrencySellOffer getCounterOffer() {
        return CurrencySellOffer.getOffer(id);
    }

    /**
     * @deprecated
     */
    long increaseSupply(long delta) {
        long excess = super.increaseSupply(delta);
        buyOfferTable.insert(this);
        return excess;
    }

    /**
     * @deprecated
     */
    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        buyOfferTable.insert(this);
    }

    /**
     * @deprecated
     */
    @Override
    public long getId() {
        return super.getId();
    }
}
