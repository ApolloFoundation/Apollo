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

package com.apollocurrency.aplwallet.apl.core.account;


import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.apollocurrency.aplwallet.apl.core.account.AccountLedger.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.app.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.app.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.Exchange;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ShufflingRecipients;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.VersionedPersistentDbTable;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

@SuppressWarnings({"UnusedDeclaration", "SuspiciousNameCombination"})
/**
 * Used as global access point to all interactions with account and public keys
 * TODO Required massive refactoring
 */
public final class Account {
    
    private static final Logger LOG = getLogger(Account.class);
    
    public enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE, CURRENCY_BALANCE, UNCONFIRMED_CURRENCY_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED, SET_PROPERTY, DELETE_PROPERTY
    }

    public enum ControlType {
        PHASING_ONLY
    }

    // TODO: YL remove static instance later

    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
   
    private static final List<Map.Entry<String, Long>> initialGenesisAccountsBalances =  Genesis.loadGenesisAccounts();
    static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    private static DatabaseManager databaseManager;


    private static final LongKeyFactory<Account> accountDbKeyFactory = new LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey == null ? newKey(account.id) : account.dbKey;
        }

        @Override
        public Account newEntity(DbKey dbKey) {
            return new Account(((DbKey.LongKey) dbKey).getId());
        }

    };
    
    private static final VersionedEntityDbTable<Account> accountTable = new AccountTable("account", accountDbKeyFactory);
    

    
    static final LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new LongKeyFactory<AccountLease>("lessor_id") {

        @Override
        public DbKey newKey(AccountLease accountLease) {
            return accountLease.dbKey;
        }

    };
    
    private static final VersionedEntityDbTable<AccountLease> accountLeaseTable = new VersionedEntityDbTable<AccountLease>("account_lease",
            accountLeaseDbKeyFactory) {
  
        @Override
        protected AccountLease load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountLease(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountLease accountLease) throws SQLException {
            accountLease.save(con);
        }

    };
    
    static final VersionedEntityDbTable<AccountInfo> accountInfoTable = new AccountInfoTable("account_info",
            AccountInfo.accountInfoDbKeyFactory, "name,description");
    
    public static final LongKeyFactory<PublicKey> publicKeyDbKeyFactory = new PublicKeyDbFactory("account_id");
    
    private static final VersionedPersistentDbTable<PublicKey> publicKeyTable = new PublicKeyTable("public_key", publicKeyDbKeyFactory);
    
    private static final VersionedPersistentDbTable<PublicKey> genesisPublicKeyTable = new PublicKeyTable("genesis_public_key", publicKeyDbKeyFactory, false);
    
    static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new AccountAssetTable("account_asset", AccountAsset.accountAssetDbKeyFactory);
    
    static final VersionedEntityDbTable<AccountCurrency> accountCurrencyTable = new AccountCurrecnyTable("account_currency", AccountCurrency.accountCurrencyDbKeyFactory);

    
    private static final VersionedEntityDbTable<AccountProperty> accountPropertyTable = new VersionedEntityDbTable<AccountProperty>("account_property", 
                                                            AccountProperty.accountPropertyDbKeyFactory) {

        @Override
        protected AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountProperty(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountProperty accountProperty) throws SQLException {
            accountProperty.save(con);
        }

    };
    
    private static final ConcurrentMap<DbKey, byte[]> publicKeyCache = propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache") ?
            new ConcurrentHashMap<>() : null;
    private static final Listeners<Account, Event> listeners = new Listeners<>();
    private static final Listeners<AccountAsset, Event> assetListeners = new Listeners<>();
    private static final Listeners<AccountCurrency, Event> currencyListeners = new Listeners<>();
    private static final Listeners<AccountLease, Event> leaseListeners = new Listeners<>();
    private static final Listeners<AccountProperty, Event> propertyListeners = new Listeners<>();


    public static void init(DatabaseManager databaseManagerParam) {
        databaseManager = databaseManagerParam;
    }
    
//TODO: move to init or constructor
    static {

        blockchainProcessor.addListener(block -> {
            int height = block.getHeight();
            List<AccountLease> changingLeases = new ArrayList<>();
            try (DbIterator<AccountLease> leases = getLeaseChangingAccounts(height)) {
                while (leases.hasNext()) {
                    changingLeases.add(leases.next());
                }
            }
            for (AccountLease lease : changingLeases) {
                Account lessor = Account.getAccount(lease.lessorId);
                if (height == lease.currentLeasingHeightFrom) {
                    lessor.activeLesseeId = lease.currentLesseeId;
                    leaseListeners.notify(lease, Event.LEASE_STARTED);
                } else if (height == lease.currentLeasingHeightTo) {
                    leaseListeners.notify(lease, Event.LEASE_ENDED);
                    lessor.activeLesseeId = 0;
                    if (lease.nextLeasingHeightFrom == 0) {
                        lease.currentLeasingHeightFrom = 0;
                        lease.currentLeasingHeightTo = 0;
                        lease.currentLesseeId = 0;
                        accountLeaseTable.delete(lease);
                    } else {
                        lease.currentLeasingHeightFrom = lease.nextLeasingHeightFrom;
                        lease.currentLeasingHeightTo = lease.nextLeasingHeightTo;
                        lease.currentLesseeId = lease.nextLesseeId;
                        lease.nextLeasingHeightFrom = 0;
                        lease.nextLeasingHeightTo = 0;
                        lease.nextLesseeId = 0;
                        accountLeaseTable.insert(lease);
                        if (height == lease.currentLeasingHeightFrom) {
                            lessor.activeLesseeId = lease.currentLesseeId;
                            leaseListeners.notify(lease, Event.LEASE_STARTED);
                        }
                    }
                }
                lessor.save();
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        if (publicKeyCache != null) {

            blockchainProcessor.addListener(block -> {
                publicKeyCache.remove(accountDbKeyFactory.newKey(block.getGeneratorId()));
                block.getTransactions().forEach(transaction -> {
                    publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getSenderId()));
                    if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                        publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getRecipientId()));
                    }
                    if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                        ShufflingRecipients shufflingRecipients = (ShufflingRecipients) transaction.getAttachment();
                        for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                            publicKeyCache.remove(accountDbKeyFactory.newKey(Account.getId(publicKey)));
                        }
                    }
                });
            }, BlockchainProcessor.Event.BLOCK_POPPED);

            blockchainProcessor.addListener(block -> publicKeyCache.clear(), BlockchainProcessor.Event.RESCAN_BEGIN);

        }

    }

    final long id;
    private final DbKey dbKey;
    private PublicKey publicKey;
    long balanceATM;
    long unconfirmedBalanceATM;
    long forgedBalanceATM;
    long activeLesseeId;
    Set<ControlType> controls;

    public Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            LOG.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.controls = Collections.emptySet();
    }

    public Account(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.balanceATM = rs.getLong("balance");
        this.unconfirmedBalanceATM = rs.getLong("unconfirmed_balance");
        this.forgedBalanceATM = rs.getLong("forged_balance");
        this.activeLesseeId = rs.getLong("active_lessee_id");
        if (rs.getBoolean("has_control_phasing")) {
            controls = Collections.unmodifiableSet(EnumSet.of(ControlType.PHASING_ONLY));
        } else {
            controls = Collections.emptySet();
        }
    }

    public static boolean addListener(Listener<Account> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Account> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.addListener(listener, eventType);
    }

    public static boolean removeAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.removeListener(listener, eventType);
    }

    public static boolean addCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.addListener(listener, eventType);
    }

    public static List<Map.Entry<String, Long>> getGenesisBalances(int firstIndex, int lastIndex) {
        firstIndex = Math.max(firstIndex, 0);
        lastIndex = Math.max(lastIndex, 0);
        if (lastIndex < firstIndex) {
            throw new IllegalArgumentException("firstIndex should be less or equal lastIndex ");
        }
        if (firstIndex >= initialGenesisAccountsBalances.size() || lastIndex > initialGenesisAccountsBalances.size()) {
            throw new IllegalArgumentException("firstIndex and lastIndex should be less than " + initialGenesisAccountsBalances.size());
        }
        if (lastIndex - firstIndex > 99) {
            lastIndex = firstIndex + 99;
        }
        return initialGenesisAccountsBalances.subList(firstIndex, lastIndex + 1);
    }

    public static int getGenesisBalancesNumber() {
        return initialGenesisAccountsBalances.size();
    }

    public static boolean removeCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.removeListener(listener, eventType);
    }

    public static boolean addLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    public static boolean removeLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    public static boolean addPropertyListener(Listener<AccountProperty> listener, Event eventType) {
        return propertyListeners.addListener(listener, eventType);
    }

    public static boolean removePropertyListener(Listener<AccountProperty> listener, Event eventType) {
        return propertyListeners.removeListener(listener, eventType);
    }

    public static int getCount() {
        return publicKeyTable.getCount() + genesisPublicKeyTable.getCount();
    }

    public static int getAssetAccountCount(long assetId) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public static int getAssetAccountCount(long assetId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId), height);
    }

    public static int getAccountAssetCount(long accountId) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountAssetCount(long accountId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static int getCurrencyAccountCount(long currencyId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    public static int getCurrencyAccountCount(long currencyId, int height) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId), height);
    }

    public static int getAccountCurrencyCount(long accountId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountCurrencyCount(long accountId, int height) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static int getAccountLeaseCount() {
        return accountLeaseTable.getCount();
    }

    public static int getActiveLeaseCount() {
        return accountTable.getCount(new DbClause.NotNullClause("active_lessee_id"));
    }

    public static AccountProperty getProperty(long propertyId) {
        return accountPropertyTable.get(AccountProperty.accountPropertyDbKeyFactory.newKey(propertyId));
    }

    public static DbIterator<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to) {
        if (recipientId == 0 && setterId == 0) {
            throw new IllegalArgumentException("At least one of recipientId and setterId must be specified");
        }
        DbClause dbClause = null;
        if (setterId == recipientId) {
            dbClause = new DbClause.NullClause("setter_id");
        } else if (setterId != 0) {
            dbClause = new DbClause.LongClause("setter_id", setterId);
        }
        if (recipientId != 0) {
            if (dbClause != null) {
                dbClause = dbClause.and(new DbClause.LongClause("recipient_id", recipientId));
            } else {
                dbClause = new DbClause.LongClause("recipient_id", recipientId);
            }
        }
        if (property != null) {
            dbClause = dbClause.and(new DbClause.StringClause("property", property));
        }
        return accountPropertyTable.getManyBy(dbClause, from, to, " ORDER BY property ");
    }

    public static AccountProperty getProperty(long recipientId, String property) {
        return getProperty(recipientId, property, recipientId);
    }

    public static AccountProperty getProperty(long recipientId, String property, long setterId) {
        if (recipientId == 0 || setterId == 0) {
            throw new IllegalArgumentException("Both recipientId and setterId must be specified");
        }
        DbClause dbClause = new DbClause.LongClause("recipient_id", recipientId);
        dbClause = dbClause.and(new DbClause.StringClause("property", property));
        if (setterId != recipientId) {
            dbClause = dbClause.and(new DbClause.LongClause("setter_id", setterId));
        } else {
            dbClause = dbClause.and(new DbClause.NullClause("setter_id"));
        }
        return accountPropertyTable.getBy(dbClause);
    }

    public static Account getAccount(long id) {
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey != null) {
                account = accountTable.newEntity(dbKey);
                account.publicKey = publicKey;
            }
        }
        return account;
    }

    public static Account getAccount(long id, int height) {
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey, height);
        if (account == null) {
            PublicKey publicKey = getPublicKey(dbKey, height);
            if (publicKey != null) {
                account = new Account(id);
                account.publicKey = publicKey;
            }
        }
        return account;
    }

    public static Account getAccount(byte[] publicKey) {
        long accountId = getId(publicKey);
        Account account = getAccount(accountId);
        if (account == null) {
            return null;
        }
        if (account.publicKey == null) {
            account.publicKey = getPublicKey(accountDbKeyFactory.newKey(account));
        }
        if (account.publicKey == null || account.publicKey.publicKey == null || Arrays.equals(account.publicKey.publicKey, publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(accountId)
                + " existing key " + Convert.toHexString(account.publicKey.publicKey) + " new key " + Convert.toHexString(publicKey));
    }

    public static DbIterator<Account> getTopHolders(Connection con, int numberOfTopAccounts) throws SQLException {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM account WHERE balance > 0 AND latest = true " +
                            " ORDER BY balance desc "+ DbUtils.limitsClause(0, numberOfTopAccounts - 1));
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            return accountTable.getManyBy(con, pstmt, false);
    }
    public static long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return getTotalAmountOnTopAccounts(con, numberOfTopAccounts);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static long getTotalAmountOnTopAccounts(Connection con, int numberOfTopAccounts) throws SQLException {
        try (
                PreparedStatement pstmt =
                        con.prepareStatement("SELECT sum(balance) as total_amount FROM (select balance from account WHERE balance > 0 AND latest = true" +
                                " ORDER BY balance desc "+ DbUtils.limitsClause(0, numberOfTopAccounts - 1)+")") ) {
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_amount");
                } else {
                    throw new RuntimeException("Cannot retrieve total_amount: no data");
                }
            }
        }
    }

    public static long getTotalAmountOnTopAccounts() {
        return getTotalAmountOnTopAccounts(100);
    }

    public static long getTotalNumberOfAccounts(Connection con) throws SQLException {
        try (
                Statement stmt =con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS number_of_accounts FROM account WHERE balance > 0 AND latest = true ")
        ) {
            if (rs.next()) {
                return rs.getLong("number_of_accounts");
            } else {
                throw new RuntimeException("Cannot retrieve number of accounts: no data");
            }
        }
    }
    public static long getTotalNumberOfAccounts() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return getTotalNumberOfAccounts(con);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    public static long getTotalSupply(Connection con) throws SQLException {
        try (
                PreparedStatement pstmt =con.prepareStatement("SELECT ABS(balance) AS total_supply FROM account WHERE id = ?")
        ) {
            int i = 0;
            pstmt.setLong(++i, Genesis.CREATOR_ID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_supply");
                } else {
                    throw new RuntimeException("Cannot retrieve total_amount: no data");
                }
            }
        }
    }
    
    public static long getTotalSupply() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return getTotalSupply(con);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    public static byte[] getPublicKey(long id) {
        DbKey dbKey = publicKeyDbKeyFactory.newKey(id);
        byte[] key = null;
        if (publicKeyCache != null) {
            key = publicKeyCache.get(dbKey);
        }
        if (key == null) {
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey == null || (key = publicKey.publicKey) == null) {
                return null;
            }
            if (publicKeyCache != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
        return key;
    }

    public static Account addOrGetAccount(long id) {
        return addOrGetAccount(id, false);
    }

    public static Account addOrGetAccount(long id, boolean isGenesis) {
        if (id == 0) {
            throw new IllegalArgumentException("Invalid accountId 0");
        }
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            account = accountTable.newEntity(dbKey);
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey == null) {
                if (isGenesis) {
                    publicKey = genesisPublicKeyTable.newEntity(dbKey);
                    genesisPublicKeyTable.insert(publicKey);
                } else {
                    publicKey = publicKeyTable.newEntity(dbKey);
                    publicKeyTable.insert(publicKey);
                }
            }
            account.publicKey = publicKey;
        }
        return account;
    }

    private static PublicKey getPublicKey(DbKey dbKey) {
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey);
        }
        return publicKey;
    }
    
    private static PublicKey getPublicKey(DbKey dbKey, boolean cache) {
        PublicKey publicKey = publicKeyTable.get(dbKey, cache);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, cache);
        }
        return publicKey;
    }

    private static PublicKey getPublicKey(DbKey dbKey, int height) {
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, height);
        }
        return publicKey;
    }

    private static DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
                            + "ORDER BY current_lessee_id, lessor_id");
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            return accountLeaseTable.getManyBy(con, pstmt, true);
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId) {
        return accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(accountId, assetId));
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId, int height) {
        return accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(accountId, assetId), height);
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId) {
        return accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId, int height) {
        return accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
    }

    public static DbIterator<AccountCurrency> getAccountCurrencies(long accountId, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<AccountCurrency> getAccountCurrencies(long accountId, int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), height, from, to);
    }

    public static long getAssetBalanceATU(long accountId, long assetId, int height) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getUnconfirmedAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
    }

    public static long getCurrencyUnits(long accountId, long currencyId, int height) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getUnconfirmedCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
    }

    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
    }

    public static EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, keySeed, publicKey);
    }

    public static byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientKeySeed, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
    }

    public static boolean setOrVerify(long accountId, byte[] key) {
        DbKey dbKey = publicKeyDbKeyFactory.newKey(accountId);
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKey.height = blockchain.getHeight();
            return true;
        }
        return Arrays.equals(publicKey.publicKey, key);
    }

    static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (accountId == Genesis.CREATOR_ID) {
            return;
        }
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed exceeds confirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
    }



    private void save() {
        if (balanceATM == 0 && unconfirmedBalanceATM == 0 && forgedBalanceATM == 0 && activeLesseeId == 0 && controls.isEmpty()) {
            accountTable.delete(this, true);
        } else {
            accountTable.insert(this);
        }
    }

    public long getId() {
        return id;
    }

    public AccountInfo getAccountInfo() {
        return accountInfoTable.get(accountDbKeyFactory.newKey(this));
    }

    public void setAccountInfo(String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo();
        if (accountInfo == null) {
            accountInfo = new AccountInfo(id, name, description);
        } else {
            accountInfo.name = name;
            accountInfo.description = description;
        }
        accountInfo.save();
    }

    public AccountLease getAccountLease() {
        return accountLeaseTable.get(accountDbKeyFactory.newKey(this));
    }

    public EncryptedData encryptTo(byte[] data, byte[] keySeed, boolean compress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return Account.encryptTo(key, data, keySeed, compress);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return Account.decryptFrom(key, encryptedData, recipientKeySeed, uncompress);
    }

    public long getBalanceATM() {
        return balanceATM;
    }

    public long getUnconfirmedBalanceATM() {
        return unconfirmedBalanceATM;
    }

    public long getForgedBalanceATM() {
        return forgedBalanceATM;
    }

    public long getEffectiveBalanceAPL() {
        return getEffectiveBalanceAPL(blockchain.getHeight());
    }

    public long getEffectiveBalanceAPL(int height) {
        if (height <= 1440) {
            Account genesisAccount = getAccount(id, 0);
            return genesisAccount == null ? 0 : genesisAccount.getBalanceATM() / Constants.ONE_APL;
        }
        if (this.publicKey == null) {
            this.publicKey = getPublicKey(accountDbKeyFactory.newKey(this));
        }
        if (this.publicKey == null || this.publicKey.publicKey == null || height - this.publicKey.height <= 1440) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }
        blockchain.readLock();
        try {
            long effectiveBalanceATM = getLessorsGuaranteedBalanceATM(height);
            if (activeLesseeId == 0) {
                effectiveBalanceATM += getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), height);
            }
            return effectiveBalanceATM < Constants.MIN_FORGING_BALANCE_ATM ? 0 : effectiveBalanceATM / Constants.ONE_APL;
        }
        finally {
            blockchain.readUnlock();
        }
    }

    private long getLessorsGuaranteedBalanceATM(int height) {
        List<Account> lessors = new ArrayList<>();
        try (DbIterator<Account> iterator = getLessors(height)) {
            while (iterator.hasNext()) {
                lessors.add(iterator.next());
            }
        }
        Long[] lessorIds = new Long[lessors.size()];
        long[] balances = new long[lessors.size()];
        for (int i = 0; i < lessors.size(); i++) {
            lessorIds[i] = lessors.get(i).getId();
            balances[i] = lessors.get(i).getBalanceATM();
        }
        int blockchainHeight = blockchain.getHeight();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS additions "
                     + "FROM account_guaranteed_balance, TABLE (id BIGINT=?) T WHERE account_id = T.id AND height > ? "
                     + (height < blockchainHeight ? " AND height <= ? " : "")
                     + " GROUP BY account_id ORDER BY account_id")) {
            pstmt.setObject(1, lessorIds);
            pstmt.setInt(2, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            if (height < blockchainHeight) {
                pstmt.setInt(3, height);
            }
            long total = 0;
            int i = 0;
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    while (lessorIds[i] < accountId && i < lessorIds.length) {
                        total += balances[i++];
                    }
                    if (lessorIds[i] == accountId) {
                        total += Math.max(balances[i++] - rs.getLong("additions"), 0);
                    }
                }
            }
            while (i < balances.length) {
                total += balances[i++];
            }
            return total;
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<Account> getLessors() {
        return accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", id), 0, -1, " ORDER BY id ASC ");
    }

    public DbIterator<Account> getLessors(int height) {
        return accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", id), height, 0, -1, " ORDER BY id ASC ");
    }

    public long getGuaranteedBalanceATM() {
        return getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), blockchain.getHeight());
    }

    public long getGuaranteedBalanceATM(final int numberOfConfirmations, final int currentHeight) {
        blockchain.readLock();
        try {
            int height = currentHeight - numberOfConfirmations;
            if (height + blockchainConfig.getGuaranteedBalanceConfirmations() < blockchainProcessor.getMinRollbackHeight()
                    || height > blockchain.getHeight()) {
                throw new IllegalArgumentException("Height " + height + " not available for guaranteed balance calculation");
            }
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                         + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height <= ?")) {
                pstmt.setLong(1, this.id);
                pstmt.setInt(2, height);
                pstmt.setInt(3, currentHeight);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return balanceATM;
                    }
                    return Math.max(Math.subtractExact(balanceATM, rs.getLong("additions")), 0);
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        finally {
            blockchain.readUnlock();
        }
    }

    public DbIterator<AccountAsset> getAssets(int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountAsset> getAssets(int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAccountTrades(this.id, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAccountAssetTransfers(this.id, from, to);
    }

    public DbIterator<CurrencyTransfer> getCurrencyTransfers(int from, int to) {
        return CurrencyTransfer.getAccountCurrencyTransfers(this.id, from, to);
    }

    public DbIterator<Exchange> getExchanges(int from, int to) {
        return Exchange.getAccountExchanges(this.id, from, to);
    }

    public AccountAsset getAsset(long assetId) {
        return accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(this.id, assetId));
    }

    public AccountAsset getAsset(long assetId, int height) {
        return accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(this.id, assetId), height);
    }

    public long getAssetBalanceATU(long assetId) {
        return getAssetBalanceATU(this.id, assetId);
    }

    public long getAssetBalanceATU(long assetId, int height) {
        return getAssetBalanceATU(this.id, assetId, height);
    }

    public long getUnconfirmedAssetBalanceATU(long assetId) {
        return getUnconfirmedAssetBalanceATU(this.id, assetId);
    }

    public AccountCurrency getCurrency(long currencyId) {
        return accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
    }

    public AccountCurrency getCurrency(long currencyId, int height) {
        return accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(this.id, currencyId), height);
    }

    public DbIterator<AccountCurrency> getCurrencies(int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountCurrency> getCurrencies(int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public long getCurrencyUnits(long currencyId) {
        return getCurrencyUnits(this.id, currencyId);
    }

    public long getCurrencyUnits(long currencyId, int height) {
        return getCurrencyUnits(this.id, currencyId, height);
    }

    public long getUnconfirmedCurrencyUnits(long currencyId) {
        return getUnconfirmedCurrencyUnits(this.id, currencyId);
    }

    public Set<ControlType> getControls() {
        return controls;
    }

    public void leaseEffectiveBalance(long lesseeId, int period) {
        int height = blockchain.getHeight();
        AccountLease accountLease = accountLeaseTable.get(accountDbKeyFactory.newKey(this));
        int leasingDelay = blockchainConfig.getLeasingDelay();
        if (accountLease == null) {
            accountLease = new AccountLease(id,
                    height + leasingDelay,
                    height + leasingDelay + period,
                    lesseeId);
        } else if (accountLease.currentLesseeId == 0) {
            accountLease.currentLeasingHeightFrom = height + leasingDelay;
            accountLease.currentLeasingHeightTo = height + leasingDelay + period;
            accountLease.currentLesseeId = lesseeId;
        } else {
            accountLease.nextLeasingHeightFrom = height + leasingDelay;
            if (accountLease.nextLeasingHeightFrom < accountLease.currentLeasingHeightTo) {
                accountLease.nextLeasingHeightFrom = accountLease.currentLeasingHeightTo;
            }
            accountLease.nextLeasingHeightTo = accountLease.nextLeasingHeightFrom + period;
            accountLease.nextLesseeId = lesseeId;
        }
        accountLeaseTable.insert(accountLease);
        leaseListeners.notify(accountLease, Event.LEASE_SCHEDULED);
    }

    public void addControl(ControlType control) {
        if (controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        accountTable.insert(this);
    }

    public void removeControl(ControlType control) {
        if (!controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.copyOf(controls);
        newControls.remove(control);
        controls = Collections.unmodifiableSet(newControls);
        save();
    }

    public void setProperty(Transaction transaction, Account setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = getProperty(this.id, property, setterAccount.id);
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), this.id, setterAccount.id, property, value);
        } else {
            accountProperty.value = value;
        }
        accountPropertyTable.insert(accountProperty);
        listeners.notify(this, Event.SET_PROPERTY);
        propertyListeners.notify(accountProperty, Event.SET_PROPERTY);
    }

   public  void deleteProperty(long propertyId) {
        AccountProperty accountProperty = accountPropertyTable.get(AccountProperty.accountPropertyDbKeyFactory.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != this.id && accountProperty.getRecipientId() != this.id) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(this.id));
        }
        accountPropertyTable.delete(accountProperty);
        listeners.notify(this, Event.DELETE_PROPERTY);
        propertyListeners.notify(accountProperty, Event.DELETE_PROPERTY);
    }

    public void apply(byte[] key) {
        apply(key, false);
    }

    public void apply(byte[] key, boolean isGenesis) {
        PublicKey publicKey = getPublicKey(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            if (isGenesis) {
                genesisPublicKeyTable.insert(publicKey);
            } else {
                publicKeyTable.insert(publicKey);
            }
        } else if (!Arrays.equals(publicKey.publicKey, key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.height >= blockchain.getHeight() - 1) {
            PublicKey dbPublicKey = getPublicKey(dbKey, false);
            if (dbPublicKey == null || dbPublicKey.publicKey == null) {
                publicKeyTable.insert(publicKey);
            }
        }
        if (publicKeyCache != null) {
            publicKeyCache.put(dbKey, key);
        }
        this.publicKey = publicKey;
    }

    public void addToAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, 0);
        } else {
            accountAsset.quantityATU = assetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id, LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

    public void addToUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(this.id, assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
        if (event == null) {
            return;
        }
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
    }

    public void addToAssetAndUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(AccountAsset.accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityATU = assetBalance;
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

    public void addToCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, 0);
        } else {
            accountCurrency.units = currencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id, LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }

    public void addToUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, 0, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
    }

    public void addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(AccountCurrency.accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.units = currencyUnits;
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                    LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }

   public  void addToBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToBalanceATM(event, eventId, amountATM, 0);
    }

    public void addToBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        this.balanceATM = Math.addExact(this.balanceATM, totalAmountATM);
        addToGuaranteedBalanceATM(totalAmountATM);
        checkBalance(this.id, this.balanceATM, this.unconfirmedBalanceATM);
        save();
        listeners.notify(this, Event.BALANCE);
        logEntryConfirmed(event, eventId, amountATM, feeATM);
    }

    public void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToUnconfirmedBalanceATM(event, eventId, amountATM, 0);
    }

    public void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        this.unconfirmedBalanceATM = Math.addExact(this.unconfirmedBalanceATM, totalAmountATM);
        checkBalance(this.id, this.balanceATM, this.unconfirmedBalanceATM);
        save();
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
        if (event == null) {
            return;
        }
        logEntryUnconfirmed(event, eventId, amountATM, feeATM);
    }

    private void logEntryUnconfirmed(LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (AccountLedger.mustLogEntry(this.id, true)) {
            if (feeATM != 0) {
                AccountLedger.logEntry(new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, this.id,
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, feeATM, this.unconfirmedBalanceATM - amountATM));
            }
            if (amountATM != 0) {
                AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                        LedgerHolding.UNCONFIRMED_APL_BALANCE, null, amountATM, this.unconfirmedBalanceATM));
            }
        }
    }

    public void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToBalanceAndUnconfirmedBalanceATM(event, eventId, amountATM, 0);
    }

    public void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (amountATM == 0 && feeATM == 0) {
            return;
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        this.balanceATM = Math.addExact(this.balanceATM, totalAmountATM);
        this.unconfirmedBalanceATM = Math.addExact(this.unconfirmedBalanceATM, totalAmountATM);
        addToGuaranteedBalanceATM(totalAmountATM);
        checkBalance(this.id, this.balanceATM, this.unconfirmedBalanceATM);
        save();
        listeners.notify(this, Event.BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
        if (event == null) {
            return;
        }
        logEntryUnconfirmed(event, eventId, amountATM, feeATM);
        logEntryConfirmed(event, eventId, amountATM, feeATM);
    }

    private void logEntryConfirmed(LedgerEvent event, long eventId, long amountATM, long feeATM) {
        if (AccountLedger.mustLogEntry(this.id, false)) {
            if (feeATM != 0) {
                AccountLedger.logEntry(new LedgerEntry(LedgerEvent.TRANSACTION_FEE, eventId, this.id,
                        LedgerHolding.APL_BALANCE, null, feeATM, this.balanceATM - amountATM));
            }
            if (amountATM != 0) {
                AccountLedger.logEntry(new LedgerEntry(event, eventId, this.id,
                        LedgerHolding.APL_BALANCE, null, amountATM, this.balanceATM));
            }
        }
    }

    public void addToForgedBalanceATM(long amountATM) {
        if (amountATM == 0) {
            return;
        }
        this.forgedBalanceATM = Math.addExact(this.forgedBalanceATM, amountATM);
        save();
    }

    private void addToGuaranteedBalanceATM(long amountATM) {
        if (amountATM <= 0) {
            return;
        }
        int blockchainHeight = blockchain.getHeight();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT additions FROM account_guaranteed_balance "
                     + "WHERE account_id = ? and height = ?");
             PreparedStatement pstmtUpdate = con.prepareStatement("MERGE INTO account_guaranteed_balance (account_id, "
                     + " additions, height) KEY (account_id, height) VALUES(?, ?, ?)")) {
            pstmtSelect.setLong(1, this.id);
            pstmtSelect.setInt(2, blockchainHeight);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                long additions = amountATM;
                if (rs.next()) {
                    additions = Math.addExact(additions, rs.getLong("additions"));
                }
                pstmtUpdate.setLong(1, this.id);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void payDividends(final long transactionId, ColoredCoinsDividendPayment attachment) {
        long totalDividend = 0;
        List<AccountAsset> accountAssets = new ArrayList<>();
        try (DbIterator<AccountAsset> iterator = getAssetAccounts(attachment.getAssetId(), attachment.getHeight(), 0, -1)) {
            while (iterator.hasNext()) {
                accountAssets.add(iterator.next());
            }
        }
        final long amountATMPerATU = attachment.getAmountATMPerATU();
        long numAccounts = 0;
        for (final AccountAsset accountAsset : accountAssets) {
            if (accountAsset.getAccountId() != this.id && accountAsset.getQuantityATU() != 0) {
                long dividend = Math.multiplyExact(accountAsset.getQuantityATU(), amountATMPerATU);
                Account.getAccount(accountAsset.getAccountId())
                        .addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, dividend);
                totalDividend += dividend;
                numAccounts += 1;
            }
        }
        this.addToBalanceATM(LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, -totalDividend);
        AssetDividend.addAssetDividend(transactionId, attachment, totalDividend, numAccounts);
    }

    @Override
    public String toString() {
        return "Account " + Long.toUnsignedString(getId());
    }

}
