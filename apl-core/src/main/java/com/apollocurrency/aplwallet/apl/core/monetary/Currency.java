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

import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountCurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@SuppressWarnings("UnusedDeclaration")
public final class Currency {

    private static final BlockChainInfoService BLOCK_CHAIN_INFO_SERVICE =
        CDI.current().select(BlockChainInfoService.class).get();
    /**
     * @deprecated
     */
    private static final LongKeyFactory<Currency> currencyDbKeyFactory = new LongKeyFactory<Currency>("id") {

        @Override
        public DbKey newKey(Currency currency) {
            return currency.dbKey == null ? newKey(currency.currencyId) : currency.dbKey;
        }

    };
    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<Currency> currencyTable = new VersionedDeletableEntityDbTable<Currency>("currency", currencyDbKeyFactory, "code,name,description") {

        @Override
        public Currency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Currency(rs, dbKey);
        }

        @Override
        public void save(Connection con, Currency currency) throws SQLException {
            currency.save(con);
        }

        @Override
        public String defaultSort() {
            return " ORDER BY creation_height DESC ";
        }

    };
    /**
     * @deprecated
     */
    private static final LongKeyFactory<CurrencySupply> currencySupplyDbKeyFactory = new LongKeyFactory<CurrencySupply>("id") {

        @Override
        public DbKey newKey(CurrencySupply currencySupply) {
            return currencySupply.dbKey;
        }

    };
    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<CurrencySupply> currencySupplyTable = new VersionedDeletableEntityDbTable<CurrencySupply>("currency_supply", currencySupplyDbKeyFactory) {

        @Override
        public CurrencySupply load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencySupply(rs, dbKey);
        }

        @Override
        public void save(Connection con, CurrencySupply currencySupply) throws SQLException {
            currencySupply.save(con);
        }

    };
    private static final Listeners<Currency, Event> listeners = new Listeners<>();
    private static AccountService accountService = CDI.current().select(AccountServiceImpl.class).get();
    private static AccountCurrencyService accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
    private static CurrencyExchangeOfferFacade currencyExchangeOfferFacade = CDI.current().select(CurrencyExchangeOfferFacade.class).get();

    private long dbId;
    private final long currencyId;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String code;
    private final String description;
    private final int type;
    private final long maxSupply;
    private final long reserveSupply;
    private final int creationHeight;
    private final int issuanceHeight;
    private final long minReservePerUnitATM;

    /*
        static {
            AplCore.getBlockchainProcessor().addListener(new CrowdFundingListener(), BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        }
    */
    private final int minDifficulty;
    private final int maxDifficulty;
    private final byte ruleset;
    private final byte algorithm;
    private final byte decimals;
    private final long initialSupply;
    private CurrencySupply currencySupply;

    /**
     * @deprecated
     */
    private Currency(Transaction transaction, MonetarySystemCurrencyIssuance attachment) {
        this.currencyId = transaction.getId();
        this.dbKey = currencyDbKeyFactory.newKey(this.currencyId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.code = attachment.getCode();
        this.description = attachment.getDescription();
        this.type = attachment.getType();
        this.initialSupply = attachment.getInitialSupply();
        this.reserveSupply = attachment.getReserveSupply();
        this.maxSupply = attachment.getMaxSupply();
        this.creationHeight = BLOCK_CHAIN_INFO_SERVICE.getHeight();
        this.issuanceHeight = attachment.getIssuanceHeight();
        this.minReservePerUnitATM = attachment.getMinReservePerUnitATM();
        this.minDifficulty = attachment.getMinDifficulty();
        this.maxDifficulty = attachment.getMaxDifficulty();
        this.ruleset = attachment.getRuleset();
        this.algorithm = attachment.getAlgorithm();
        this.decimals = attachment.getDecimals();
    }

    /**
     * @deprecated
     */
    private Currency(ResultSet rs, DbKey dbKey) throws SQLException {
        this.dbId = rs.getLong("db_id");
        this.currencyId = rs.getLong("id");
        this.dbKey = dbKey;
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.code = rs.getString("code");
        this.description = rs.getString("description");
        this.type = rs.getInt("type");
        this.initialSupply = rs.getLong("initial_supply");
        this.reserveSupply = rs.getLong("reserve_supply");
        this.maxSupply = rs.getLong("max_supply");
        this.creationHeight = rs.getInt("creation_height");
        this.issuanceHeight = rs.getInt("issuance_height");
        this.minReservePerUnitATM = rs.getLong("min_reserve_per_unit_atm");
        this.minDifficulty = rs.getByte("min_difficulty") & 0xFF;
        this.maxDifficulty = rs.getByte("max_difficulty") & 0xFF;
        this.ruleset = rs.getByte("ruleset");
        this.algorithm = rs.getByte("algorithm");
        this.decimals = rs.getByte("decimals");
    }

    /**
     * @deprecated use for unit test  only
     */
    public Currency(long currencyId, DbKey dbKey, long accountId, String name, String code, String description,
                     int type, long maxSupply, long reserveSupply, int creationHeight, int issuanceHeight,
                     long minReservePerUnitATM, int minDifficulty, int maxDifficulty, byte ruleset, byte algorithm, byte decimals, long initialSupply) {
        this.currencyId = currencyId;
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.type = type;
        this.maxSupply = maxSupply;
        this.reserveSupply = reserveSupply;
        this.creationHeight = creationHeight;
        this.issuanceHeight = issuanceHeight;
        this.minReservePerUnitATM = minReservePerUnitATM;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
        this.ruleset = ruleset;
        this.algorithm = algorithm;
        this.decimals = decimals;
        this.initialSupply = initialSupply;
    }

    /**
     * @deprecated
     */
    public static boolean addListener(Listener<Currency> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    /**
     * @deprecated
     */
    public static boolean removeListener(Listener<Currency> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    /**
     * @deprecated
     */
    public static DbIterator<Currency> getAllCurrencies(int from, int to) {
        return currencyTable.getAll(from, to);
    }

    /**
     * @deprecated
     */
    public static int getCount() {
        return currencyTable.getCount();
    }

    /**
     * @deprecated
     */
    public static Currency getCurrency(long id) {
        return currencyTable.get(currencyDbKeyFactory.newKey(id));
    }

    /**
     * @deprecated
     */
    public static Currency getCurrencyByName(String name) {
        return currencyTable.getBy(new DbClause.StringClause("name_lower", name.toLowerCase()));
    }

    /**
     * @deprecated
     */
    public static Currency getCurrencyByCode(String code) {
        return currencyTable.getBy(new DbClause.StringClause("code", code.toUpperCase()));
    }

    /**
     * @deprecated
     */
    public static DbIterator<Currency> getCurrencyIssuedBy(long accountId, int from, int to) {
        return currencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<Currency> searchCurrencies(String query, int from, int to) {
        return currencyTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, currency.creation_height DESC ");
    }

    /**
     * @deprecated
     */
    static void addCurrency(LedgerEvent event, long eventId, Transaction transaction, Account senderAccount,
                            MonetarySystemCurrencyIssuance attachment) {
        Currency oldCurrency;
        if ((oldCurrency = Currency.getCurrencyByCode(attachment.getCode())) != null) {
            oldCurrency.delete(event, eventId, senderAccount);
        }
        if ((oldCurrency = Currency.getCurrencyByCode(attachment.getName())) != null) {
            oldCurrency.delete(event, eventId, senderAccount);
        }
        if ((oldCurrency = Currency.getCurrencyByName(attachment.getName())) != null) {
            oldCurrency.delete(event, eventId, senderAccount);
        }
        if ((oldCurrency = Currency.getCurrencyByName(attachment.getCode())) != null) {
            oldCurrency.delete(event, eventId, senderAccount);
        }
        Currency currency = new Currency(transaction, attachment);
        currencyTable.insert(currency);
        if (currency.is(CurrencyType.MINTABLE) || currency.is(CurrencyType.RESERVABLE)) {
            CurrencySupply currencySupply = currency.getSupplyData();
            currencySupply.currentSupply = attachment.getInitialSupply();
            currencySupplyTable.insert(currencySupply);
        }

    }

    /**
     * @deprecated
     */
    public static void init() {
    }

    /**
     * @deprecated
     */
    static void increaseReserve(LedgerEvent event, long eventId, Account account, long currencyId, long amountPerUnitATM) {
        Currency currency = Currency.getCurrency(currencyId);
        accountService.addToBalanceATM(account, event, eventId, -Math.multiplyExact(currency.getReserveSupply(), amountPerUnitATM));
        CurrencySupply currencySupply = currency.getSupplyData();
        currencySupply.currentReservePerUnitATM += amountPerUnitATM;
        currencySupplyTable.insert(currencySupply);
        CurrencyFounder.addOrUpdateFounder(currencyId, account.getId(), amountPerUnitATM);
    }

    /**
     * @deprecated
     */
    static void claimReserve(LedgerEvent event, long eventId, Account account, long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(account, event, eventId, currencyId, -units);
        Currency currency = Currency.getCurrency(currencyId);
        currency.increaseSupply(-units);
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId,
            Math.multiplyExact(units, currency.getCurrentReservePerUnitATM()));
    }

    /**
     * @deprecated
     */
    static void transferCurrency(LedgerEvent event, long eventId, Account senderAccount, Account recipientAccount,
                                 long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(senderAccount, event, eventId, currencyId, -units);
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(recipientAccount, event, eventId, currencyId, units);
    }

    private void save(Connection con) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency (id, account_id, name, code, "
                + "description, type, initial_supply, reserve_supply, max_supply, creation_height, issuance_height, min_reserve_per_unit_atm, "
                + "min_difficulty, max_difficulty, ruleset, algorithm, decimals, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, this.currencyId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.code);
            pstmt.setString(++i, this.description);
            pstmt.setInt(++i, this.type);
            pstmt.setLong(++i, this.initialSupply);
            pstmt.setLong(++i, this.reserveSupply);
            pstmt.setLong(++i, this.maxSupply);
            pstmt.setInt(++i, this.creationHeight);
            pstmt.setInt(++i, this.issuanceHeight);
            pstmt.setLong(++i, this.minReservePerUnitATM);
            pstmt.setByte(++i, (byte) this.minDifficulty);
            pstmt.setByte(++i, (byte) this.maxDifficulty);
            pstmt.setByte(++i, this.ruleset);
            pstmt.setByte(++i, this.algorithm);
            pstmt.setByte(++i, this.decimals);
            pstmt.setInt(++i, BLOCK_CHAIN_INFO_SERVICE.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getDbId() {
        return dbId;
    }

    public long getId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getType() {
        return type;
    }

    public long getInitialSupply() {
        return initialSupply;
    }

    /**
     * @deprecated
     */
    public long getCurrentSupply() {
        if (!is(CurrencyType.RESERVABLE) && !is(CurrencyType.MINTABLE)) {
            return initialSupply;
        }
        if (getSupplyData() == null) {
            return 0;
        }
        return currencySupply.currentSupply;
    }

    public long getReserveSupply() {
        return reserveSupply;
    }

    public long getMaxSupply() {
        return maxSupply;
    }

    public int getCreationHeight() {
        return creationHeight;
    }

    public int getIssuanceHeight() {
        return issuanceHeight;
    }

    public long getMinReservePerUnitATM() {
        return minReservePerUnitATM;
    }

    public int getMinDifficulty() {
        return minDifficulty;
    }

    public int getMaxDifficulty() {
        return maxDifficulty;
    }

    public byte getRuleset() {
        return ruleset;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public byte getDecimals() {
        return decimals;
    }

    /**
     * @deprecated
     */
    public long getCurrentReservePerUnitATM() {
        if (!is(CurrencyType.RESERVABLE) || getSupplyData() == null) {
            return 0;
        }
        return currencySupply.currentReservePerUnitATM;
    }

    /**
     * @deprecated
     */
    public boolean isActive() {
        return issuanceHeight <= BLOCK_CHAIN_INFO_SERVICE.getHeight();
    }

    /**
     * @deprecated
     */
    private CurrencySupply getSupplyData() {
        if (!is(CurrencyType.RESERVABLE) && !is(CurrencyType.MINTABLE)) {
            return null;
        }
        if (currencySupply == null) {
            currencySupply = currencySupplyTable.get(currencyDbKeyFactory.newKey(this));
            if (currencySupply == null) {
                currencySupply = new CurrencySupply(this);
            }
        }
        return currencySupply;
    }

    /**
     * @deprecated
     */
    public void increaseSupply(long units) {
        getSupplyData();
        currencySupply.currentSupply += units;
        if (currencySupply.currentSupply > maxSupply || currencySupply.currentSupply < 0) {
            currencySupply.currentSupply -= units;
            throw new IllegalArgumentException("Cannot add " + units + " to current supply of " + currencySupply.currentSupply);
        }
        currencySupplyTable.insert(currencySupply);
    }

    /**
     * @deprecated
     */
    public DbIterator<Exchange> getExchanges(int from, int to) {
        return Exchange.getCurrencyExchanges(this.currencyId, from, to);
    }

    /**
     * @deprecated
     */
    public DbIterator<CurrencyTransfer> getTransfers(int from, int to) {
        return CurrencyTransfer.getCurrencyTransfers(this.currencyId, from, to);
    }

    /**
     * @deprecated
     */
    public boolean is(CurrencyType type) {
        return (this.type & type.getCode()) != 0;
    }

    /**
     * @deprecated
     */
    public boolean canBeDeletedBy(long senderAccountId) {
//        if (!is(CurrencyType.NON_SHUFFLEABLE) && Shuffling.getHoldingShufflingCount(currencyId, false) > 0) {
//            return false;
//        }
//        if (!isActive()) {
//            return senderAccountId == accountId;
//        }
//        if (is(CurrencyType.MINTABLE) && getCurrentSupply() < maxSupply && senderAccountId != accountId) {
//            return false;
//        }
//
//        List<AccountCurrency> accountCurrencies = accountCurrencyService.getCurrenciesByAccount(this.currencyId, 0, -1);
//        return accountCurrencies.isEmpty() || accountCurrencies.size() == 1 && accountCurrencies.get(0).getAccountId() == senderAccountId;
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     */
    void delete(LedgerEvent event, long eventId, Account senderAccount) {
        if (!canBeDeletedBy(senderAccount.getId())) {
            // shouldn't happen as ownership has already been checked in validate, but as a safety check
            throw new IllegalStateException("Currency " + Long.toUnsignedString(currencyId) + " not entirely owned by " + Long.toUnsignedString(senderAccount.getId()));
        }
        listeners.notify(this, Event.BEFORE_DELETE);
        if (is(CurrencyType.RESERVABLE)) {
            if (is(CurrencyType.CLAIMABLE) && isActive()) {
                accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, event, eventId, currencyId,
                    -accountCurrencyService.getCurrencyUnits(senderAccount, currencyId));
                Currency.claimReserve(event, eventId, senderAccount, currencyId,
                    accountCurrencyService.getCurrencyUnits(senderAccount, currencyId));
            }
            if (!isActive()) {
                try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE)) {
                    for (CurrencyFounder founder : founders) {
                        accountService.addToBalanceAndUnconfirmedBalanceATM(
                            accountService.getAccount(founder.getAccountId()),
                            event, eventId, Math.multiplyExact(reserveSupply, founder.getAmountPerUnitATM()));
                    }
                }
            }
            CurrencyFounder.remove(currencyId);
        }
        if (is(CurrencyType.EXCHANGEABLE)) {
            List<CurrencyBuyOffer> buyOffers = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getOffers(this, 0, -1)) {
                while (offers.hasNext()) {
                    buyOffers.add(offers.next());
                }
            }
            buyOffers.forEach((offer) -> CurrencyExchangeOffer.removeOffer(event, offer));
        }
        if (is(CurrencyType.MINTABLE)) {
            CurrencyMint.deleteCurrency(this);
        }
        accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, event, eventId, currencyId,
            -accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, currencyId));
        accountCurrencyService.addToCurrencyUnits(senderAccount, event, eventId, currencyId,
            -accountCurrencyService.getCurrencyUnits(senderAccount, currencyId));
        currencyTable.deleteAtHeight(this, BLOCK_CHAIN_INFO_SERVICE.getHeight());
    }

    public enum Event {
        BEFORE_DISTRIBUTE_CROWDFUNDING, BEFORE_UNDO_CROWDFUNDING, BEFORE_DELETE
    }

    private static final class CurrencySupply {

        private long dbId;
        private final DbKey dbKey;
        private final long currencyId;
        private long currentSupply;
        private long currentReservePerUnitATM;

        private CurrencySupply(Currency currency) {
            this.currencyId = currency.currencyId;
            this.dbKey = currencySupplyDbKeyFactory.newKey(this.currencyId);
        }

        private CurrencySupply(ResultSet rs, DbKey dbKey) throws SQLException {
            this.dbId = rs.getLong("db_id");
            this.currencyId = rs.getLong("id");
            this.dbKey = dbKey;
            this.currentSupply = rs.getLong("current_supply");
            this.currentReservePerUnitATM =
                rs.getLong("current_reserve_per_unit_atm");
        }

        private void save(Connection con) throws SQLException {
            try (
                @DatabaseSpecificDml(DmlMarker.MERGE)
                PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_supply (id, current_supply, "
                    + "current_reserve_per_unit_atm, height, latest, deleted) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, TRUE, FALSE)")
            ) {
                int i = 0;
                pstmt.setLong(++i, this.currencyId);
                pstmt.setLong(++i, this.currentSupply);
                pstmt.setLong(++i, this.currentReservePerUnitATM);
                pstmt.setInt(++i, BLOCK_CHAIN_INFO_SERVICE.getHeight());
                pstmt.executeUpdate();
            }
        }
        public long getDbId() {
            return dbId;
        }
    }

/*
    @Slf4j
    public static class CrowdFundingListener {
        public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
            log.trace(":accept:CrowdFundingListener: START onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
            try (DbIterator<Currency> issuedCurrencies = currencyTable.getManyBy(new DbClause.IntClause("issuance_height", block.getHeight()), 0, -1)) {
                for (Currency currency : issuedCurrencies) {
                    if (currency.getCurrentReservePerUnitATM() < currency.getMinReservePerUnitATM()) {
                        listeners.notify(currency, Event.BEFORE_UNDO_CROWDFUNDING);
                        undoCrowdFunding(currency);
                    } else {
                        listeners.notify(currency, Event.BEFORE_DISTRIBUTE_CROWDFUNDING);
                        distributeCurrency(currency);
                    }
                }
            }
            log.trace(":accept:CrowdFundingListener: END onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
        }

        private void undoCrowdFunding(Currency currency) {
            try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                for (CurrencyFounder founder : founders) {
                    accountService.addToBalanceAndUnconfirmedBalanceATM(
                        accountService.getAccount(founder.getAccountId()),
                        LedgerEvent.CURRENCY_UNDO_CROWDFUNDING,
                        currency.getId(),
                        Math.multiplyExact(currency.getReserveSupply(),
                            founder.getAmountPerUnitATM()));
                }
            }
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                accountService.getAccount(currency.getAccountId()),
                LedgerEvent.CURRENCY_UNDO_CROWDFUNDING, currency.getId(),
                currency.getId(), -currency.getInitialSupply());
            currencyTable.deleteAtHeight(currency, BLOCK_CHAIN_INFO_SERVICE.getHeight());
            CurrencyFounder.remove(currency.getId());
        }

        private void distributeCurrency(Currency currency) {
            long totalAmountPerUnit = 0;
            final long remainingSupply = currency.getReserveSupply() - currency.getInitialSupply();
            List<CurrencyFounder> currencyFounders = new ArrayList<>();
            try (DbIterator<CurrencyFounder> founders = CurrencyFounder.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                for (CurrencyFounder founder : founders) {
                    totalAmountPerUnit += founder.getAmountPerUnitATM();
                    currencyFounders.add(founder);
                }
            }
            CurrencySupply currencySupply = currency.getSupplyData();
            for (CurrencyFounder founder : currencyFounders) {
                long units = Math.multiplyExact(remainingSupply, founder.getAmountPerUnitATM()) / totalAmountPerUnit;
                currencySupply.currentSupply += units;
                accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                    accountService.getAccount(founder.getAccountId()),
                    LedgerEvent.CURRENCY_DISTRIBUTION, currency.getId(),
                    currency.getId(), units);
            }
            Account issuerAccount = accountService.getAccount(currency.getAccountId());
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getId(),
                currency.getId(), currency.getReserveSupply() - currency.getCurrentSupply());
            if (!currency.is(CurrencyType.CLAIMABLE)) {
                accountService.addToBalanceAndUnconfirmedBalanceATM(issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getId(),
                    Math.multiplyExact(totalAmountPerUnit, currency.getReserveSupply()));
            }
            currencySupply.currentSupply = currency.getReserveSupply();
            currencySupplyTable.insert(currencySupply);
        }
    }
*/
}
