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

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class CurrencyExchangeOffer {

    static final DbClause availableOnlyDbClause = new DbClause.LongClause("unit_limit", DbClause.Op.NE, 0)
        .and(new DbClause.LongClause("supply", DbClause.Op.NE, 0));
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static AccountService accountService = CDI.current().select(AccountServiceImpl.class).get();
    private static AccountCurrencyService accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
    final long id;
    private final long currencyId;
    private final long accountId;
    private final long rateATM;
    private final int expirationHeight;
    private final int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;
    private long limit; // limit on the total sum of units for this offer across transactions
    private long supply; // total units supply for the offer

    CurrencyExchangeOffer(long id, long currencyId, long accountId, long rateATM, long limit, long supply,
                          int expirationHeight, int transactionHeight, short transactionIndex) {
        this.id = id;
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.rateATM = rateATM;
        this.limit = limit;
        this.supply = supply;
        this.expirationHeight = expirationHeight;
        this.creationHeight = blockchain.getHeight();
        this.transactionIndex = transactionIndex;
        this.transactionHeight = transactionHeight;
    }

    CurrencyExchangeOffer(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.rateATM = rs.getLong("rate");
        this.limit = rs.getLong("unit_limit");
        this.supply = rs.getLong("supply");
        this.expirationHeight = rs.getInt("expiration_height");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
    }

    //    static {
    public static void init() {
    }

    static void publishOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment) {
        CurrencyBuyOffer previousOffer = CurrencyBuyOffer.getOffer(attachment.getCurrencyId(), transaction.getSenderId());
        if (previousOffer != null) {
            CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, previousOffer);
        }
        CurrencyBuyOffer.addOffer(transaction, attachment);
        CurrencySellOffer.addOffer(transaction, attachment);
    }

    private static AvailableOffers calculateTotal(List<CurrencyExchangeOffer> offers, final long units) {
        long totalAmountATM = 0;
        long remainingUnits = units;
        long rateATM = 0;
        for (CurrencyExchangeOffer offer : offers) {
            if (remainingUnits == 0) {
                break;
            }
            rateATM = offer.getRateATM();
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, offer.getRateATM());
            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);
        }
        return new AvailableOffers(rateATM, Math.subtractExact(units, remainingUnits), totalAmountATM);
    }

    public static AvailableOffers getAvailableToSell(final long currencyId, final long units) {
        return calculateTotal(getAvailableBuyOffers(currencyId, 0L), units);
    }

    private static List<CurrencyExchangeOffer> getAvailableBuyOffers(long currencyId, long minRateATM) {
        List<CurrencyExchangeOffer> currencyExchangeOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (minRateATM > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.GTE, minRateATM));
        }
        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(dbClause, 0, -1,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencyBuyOffer offer : offers) {
                currencyExchangeOffers.add(offer);
            }
        }
        return currencyExchangeOffers;
    }

    static void exchangeCurrencyForAPL(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units) {
        List<CurrencyExchangeOffer> currencyBuyOffers = getAvailableBuyOffers(currencyId, rateATM);
        log.trace("account === 0 exchangeCurrencyForAPL account={}, currencyId={}, rateATM={}, units={}", account, currencyId, rateATM, units);
        long totalAmountATM = 0;
        long remainingUnits = units;
        for (CurrencyExchangeOffer offer : currencyBuyOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, offer.getRateATM());

            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = accountService.getAccount(offer.getAccountId());
            log.trace("account === 1 exchangeCurrencyForAPL account={}", counterAccount);
            accountService.addToBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), -curAmountATM);
            accountCurrencyService.addToCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, curUnits);
            accountCurrencyService.addToUnconfirmedCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, excess);
            log.trace("account === 2 exchangeCurrencyForAPL account={}", counterAccount);
            Exchange.addExchange(transaction, currencyId, offer, account.getId(),
                offer.getAccountId(), curUnits, blockchain.getLastBlock());
        }
        long transactionId = transaction.getId();
        account = accountService.getAccount(account);
        log.trace("account === 3 exchangeCurrencyForAPL account={}", account);
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, totalAmountATM);
        accountCurrencyService.addToCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, -(units - remainingUnits));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, currencyId, remainingUnits);
        log.trace("account === 4 exchangeCurrencyForAPL account={}", account);
    }

    public static AvailableOffers getAvailableToBuy(final long currencyId, final long units) {
        return calculateTotal(getAvailableSellOffers(currencyId, 0L), units);
    }

    private static List<CurrencyExchangeOffer> getAvailableSellOffers(long currencyId, long maxRateATM) {
        List<CurrencyExchangeOffer> currencySellOffers = new ArrayList<>();
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId).and(availableOnlyDbClause);
        if (maxRateATM > 0) {
            dbClause = dbClause.and(new DbClause.LongClause("rate", DbClause.Op.LTE, maxRateATM));
        }
        try (DbIterator<CurrencySellOffer> offers = CurrencySellOffer.getOffers(dbClause, 0, -1,
            " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ")) {
            for (CurrencySellOffer offer : offers) {
                currencySellOffers.add(offer);
            }
        }
        return currencySellOffers;
    }

    static void exchangeAPLForCurrency(Transaction transaction, Account account, final long currencyId, final long rateATM, final long units) {
        List<CurrencyExchangeOffer> currencySellOffers = getAvailableSellOffers(currencyId, rateATM);
        long totalAmountATM = 0;
        long remainingUnits = units;
        log.trace("account === 0 exchangeAPLForCurrency account={}, currencyId={}, rateATM={}, units={}", account, currencyId, rateATM, units);
        for (CurrencyExchangeOffer offer : currencySellOffers) {
            if (remainingUnits == 0) {
                break;
            }
            long curUnits = Math.min(Math.min(remainingUnits, offer.getSupply()), offer.getLimit());
            long curAmountATM = Math.multiplyExact(curUnits, offer.getRateATM());

            totalAmountATM = Math.addExact(totalAmountATM, curAmountATM);
            remainingUnits = Math.subtractExact(remainingUnits, curUnits);

            offer.decreaseLimitAndSupply(curUnits);
            long excess = offer.getCounterOffer().increaseSupply(curUnits);

            Account counterAccount = accountService.getAccount(offer.getAccountId());
            log.trace("account === 1 exchangeAPLForCurrency account={}", counterAccount);
            accountService.addToBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), curAmountATM);
            accountService.addToUnconfirmedBalanceATM(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(),
                Math.addExact(
                    Math.multiplyExact(curUnits - excess, offer.getRateATM() - offer.getCounterOffer().getRateATM()),
                    Math.multiplyExact(excess, offer.getRateATM())
                )
            );
            accountCurrencyService.addToCurrencyUnits(counterAccount, LedgerEvent.CURRENCY_EXCHANGE, offer.getId(), currencyId, -curUnits);
            log.trace("account === 2 exchangeAPLForCurrency account={}", counterAccount);
            Exchange.addExchange(transaction, currencyId, offer, offer.getAccountId(),
                account.getId(), curUnits, blockchain.getLastBlock());
        }
        long transactionId = transaction.getId();
        account = accountService.getAccount(account);
        log.trace("account === 3 exchangeAPLForCurrency account={}", account);
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId,
            currencyId, Math.subtractExact(units, remainingUnits));
        accountService.addToBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, -totalAmountATM);
        accountService.addToUnconfirmedBalanceATM(account, LedgerEvent.CURRENCY_EXCHANGE, transactionId, Math.multiplyExact(units, rateATM) - totalAmountATM);
        log.trace("account === 4 exchangeAPLForCurrency account={}", account);
    }

    static void removeOffer(LedgerEvent event, CurrencyBuyOffer buyOffer) {
        CurrencySellOffer sellOffer = buyOffer.getCounterOffer();

        CurrencyBuyOffer.remove(buyOffer);
        CurrencySellOffer.remove(sellOffer);

        Account account = accountService.getAccount(buyOffer.getAccountId());
        accountService.addToUnconfirmedBalanceATM(account, event, buyOffer.getId(), Math.multiplyExact(buyOffer.getSupply(), buyOffer.getRateATM()));
        accountCurrencyService.addToUnconfirmedCurrencyUnits(account, event, buyOffer.getId(), buyOffer.getCurrencyId(), sellOffer.getSupply());
    }

    void save(Connection con, String table) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, currency_id, account_id, "
                + "rate, unit_limit, supply, expiration_height, creation_height, transaction_index, transaction_height, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.currencyId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.rateATM);
            pstmt.setLong(++i, this.limit);
            pstmt.setLong(++i, this.supply);
            pstmt.setInt(++i, this.expirationHeight);
            pstmt.setInt(++i, this.creationHeight);
            pstmt.setShort(++i, this.transactionIndex);
            pstmt.setInt(++i, this.transactionHeight);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getRateATM() {
        return rateATM;
    }

    public long getLimit() {
        return limit;
    }

    public long getSupply() {
        return supply;
    }

    public int getExpirationHeight() {
        return expirationHeight;
    }

    public int getHeight() {
        return creationHeight;
    }

    public abstract CurrencyExchangeOffer getCounterOffer();

    long increaseSupply(long delta) {
        long excess = Math.max(Math.addExact(supply, Math.subtractExact(delta, limit)), 0);
        supply += delta - excess;
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        limit -= delta;
        supply -= delta;
    }

    @Override
    public String toString() {
        return "CurrencyExchangeOffer{" +
            "id=" + id +
            ", currencyId=" + currencyId +
            ", accountId=" + accountId +
            ", rateATM=" + rateATM +
            ", limit=" + limit +
            ", supply=" + supply +
            ", expirationHeight=" + expirationHeight +
            ", creationHeight=" + creationHeight +
            ", transactionIndex=" + transactionIndex +
            ", transactionHeight=" + transactionHeight +
            '}';
    }

    public static final class AvailableOffers {

        private final long rateATM;
        private final long units;
        private final long amountATM;

        private AvailableOffers(long rateATM, long units, long amountATM) {
            this.rateATM = rateATM;
            this.units = units;
            this.amountATM = amountATM;
        }

        public long getRateATM() {
            return rateATM;
        }

        public long getUnits() {
            return units;
        }

        public long getAmountATM() {
            return amountATM;
        }

    }

    @Singleton
    @Slf4j
    public static class CurrencyExchangeOfferObserver {
        public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
            log.trace(":accept:CurrencyExchangeOfferObserver: START onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
            List<CurrencyBuyOffer> expired = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(new DbClause.IntClause("expiration_height", block.getHeight()), 0, -1)) {
                for (CurrencyBuyOffer offer : offers) {
                    expired.add(offer);
                }
            }
            expired.forEach((offer) -> CurrencyExchangeOffer.removeOffer(LedgerEvent.CURRENCY_OFFER_EXPIRED, offer));
            log.trace(":accept:CurrencyExchangeOfferObserver: END onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
        }
    }
}
