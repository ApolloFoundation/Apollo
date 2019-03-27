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

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.apollocurrency.aplwallet.apl.core.monetary.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

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

    static BlockchainConfig blockchainConfig;
    static Blockchain blockchain;
    private static BlockchainProcessor blockchainProcessor;
    private static DatabaseManager databaseManager;
    private static GlobalSync sync;
    private static PublicKeyTable publicKeyTable;
    private static  ConcurrentMap<DbKey, byte[]> publicKeyCache = null; 
           
    
    private static final Listeners<Account, Event> listeners = new Listeners<>();
    private static final Listeners<AccountAsset, Event> assetListeners = new Listeners<>();
    private static final Listeners<AccountCurrency, Event> currencyListeners = new Listeners<>();
    private static final Listeners<AccountLease, Event> leaseListeners = new Listeners<>();
    private static final Listeners<AccountProperty, Event> propertyListeners = new Listeners<>();

    final long id;
    DbKey dbKey;
    private PublicKey publicKey;
    long balanceATM;
    long unconfirmedBalanceATM;
    long forgedBalanceATM;
    long activeLesseeId;
    Set<ControlType> controls;

    public static void init(DatabaseManager databaseManagerParam,
                            PropertiesHolder propertiesHolder,
                            BlockchainProcessor blockchainProcessorParam,
                            BlockchainConfig blockchainConfigParam,
                            Blockchain blockchainParam,
                            GlobalSync globalSync,
                            PublicKeyTable pkTable
    ) {
        databaseManager = databaseManagerParam;
        blockchainProcessor = blockchainProcessorParam;
        blockchainConfig = blockchainConfigParam;
        blockchain = blockchainParam;
        publicKeyTable = pkTable;
        sync = globalSync;

        if (propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache")) {
            publicKeyCache = new ConcurrentHashMap<>();
        }
        }


    @Singleton
    public static class AccountObserver {

        public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
            if (publicKeyCache != null) {
                publicKeyCache.clear();
            }
        }

        public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
            if (publicKeyCache != null) {
                publicKeyCache.remove(AccountTable.newKey(block.getGeneratorId()));
                block.getTransactions().forEach(transaction -> {
                    publicKeyCache.remove(AccountTable.newKey(transaction.getSenderId()));
                    if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                        publicKeyCache.remove(AccountTable.newKey(transaction.getRecipientId()));
                    }
                    if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                        ShufflingRecipientsAttachment shufflingRecipients = (ShufflingRecipientsAttachment) transaction.getAttachment();
                        for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                            publicKeyCache.remove(AccountTable.newKey(Account.getId(publicKey)));
                        }
                    }
                });
            }
        }

        public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
            int height = block.getHeight();
            List<AccountLease> changingLeases = new ArrayList<>();
            try (DbIterator<AccountLease> leases = AccountLeaseTable.getInstance().getLeaseChangingAccounts(height)) {
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
                        AccountLeaseTable.getInstance().delete(lease);
                    } else {
                        lease.currentLeasingHeightFrom = lease.nextLeasingHeightFrom;
                        lease.currentLeasingHeightTo = lease.nextLeasingHeightTo;
                        lease.currentLesseeId = lease.nextLesseeId;
                        lease.nextLeasingHeightFrom = 0;
                        lease.nextLeasingHeightTo = 0;
                        lease.nextLesseeId = 0;
                        AccountLeaseTable.getInstance().insert(lease);
                        if (height == lease.currentLeasingHeightFrom) {
                            lessor.activeLesseeId = lease.currentLesseeId;
                            leaseListeners.notify(lease, Event.LEASE_STARTED);
                        }
                    }
                }
                lessor.save();
            }
        }
    }


    public Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            LOG.info("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = AccountTable.newKey(this.id);
        this.controls = Collections.emptySet();
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
        return publicKeyTable.getCount() + GenesisPublicKeyTable.getInstance().getCount();
    }

    public static int getActiveLeaseCount() {
        return AccountTable.getInstance().getCount(new DbClause.NotNullClause("active_lessee_id"));
    }

    public static Account getAccount(long id) {
        DbKey dbKey = AccountTable.newKey(id);
        Account account = AccountTable.getInstance().get(dbKey);
        if (account == null) {
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey != null) {
                account = AccountTable.getInstance().newEntity(dbKey);
                account.publicKey = publicKey;
            }
        }
        return account;
    }

    public static Account getAccount(long id, int height) {
        DbKey dbKey = AccountTable.newKey(id);
        Account account = AccountTable.getInstance().get(dbKey, height);
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
            account.publicKey = getPublicKey(AccountTable.newKey(account));
        }
        if (account.publicKey == null || account.publicKey.publicKey == null || Arrays.equals(account.publicKey.publicKey, publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(accountId)
                + " existing key " + Convert.toHexString(account.publicKey.publicKey) + " new key " + Convert.toHexString(publicKey));
    }
    
    public static long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalAmountOnTopAccounts(con, numberOfTopAccounts);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static long getTotalAmountOnTopAccounts() {
        return getTotalAmountOnTopAccounts(100);
    } 

    
    public static long getTotalNumberOfAccounts() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalNumberOfAccounts(con);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    
    public static long getTotalSupply() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection()) {
            return AccountTable.getTotalSupply(con);
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
        DbKey dbKey = PublicKeyTable.newKey(id);
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
        DbKey dbKey = AccountTable.newKey(id);
        Account account = AccountTable.getInstance().get(dbKey);
        if (account == null) {
            account = AccountTable.getInstance().newEntity(dbKey);
            PublicKey publicKey = getPublicKey(dbKey);
            if (publicKey == null) {
                if (isGenesis) {
                    publicKey = GenesisPublicKeyTable.getInstance().newEntity(dbKey);
                    GenesisPublicKeyTable.getInstance().insert(publicKey);
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
            publicKey = GenesisPublicKeyTable.getInstance().get(dbKey);
        }
        return publicKey;
    }
    
    private static PublicKey getPublicKey(DbKey dbKey, boolean cache) {
        PublicKey publicKey = publicKeyTable.get(dbKey, cache);
        if (publicKey == null) {
            publicKey = GenesisPublicKeyTable.getInstance().get(dbKey, cache);
        }
        return publicKey;
    }

    private static PublicKey getPublicKey(DbKey dbKey, int height) {
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = GenesisPublicKeyTable.getInstance().get(dbKey, height);
        }
        return publicKey;
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
        DbKey dbKey = PublicKeyTable.newKey(accountId);
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
            AccountTable.getInstance().delete(this, true);
        } else {
            AccountTable.getInstance().insert(this);
        }
    }

    public long getId() {
        return id;
    }

    public AccountInfo getAccountInfo() {
        return AccountInfoTable.getInstance().get(AccountTable.newKey(this));
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
        return AccountLeaseTable.getInstance().get(AccountTable.newKey(this));
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
            this.publicKey = getPublicKey(AccountTable.newKey(this));
        }
        if (this.publicKey == null || this.publicKey.publicKey == null || height - this.publicKey.height <= 1440) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }
        sync.readLock();
        try {
            long effectiveBalanceATM = getLessorsGuaranteedBalanceATM(height);
            if (activeLesseeId == 0) {
                effectiveBalanceATM += getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), height);
            }
            return effectiveBalanceATM < Constants.MIN_FORGING_BALANCE_ATM ? 0 : effectiveBalanceATM / Constants.ONE_APL;
        }
        finally {
            sync.readUnlock();
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
        return AccountTable.getInstance().getManyBy(new DbClause.LongClause("active_lessee_id", id), 0, -1, " ORDER BY id ASC ");
    }

    public DbIterator<Account> getLessors(int height) {
        return AccountTable.getInstance().getManyBy(new DbClause.LongClause("active_lessee_id", id), height, 0, -1, " ORDER BY id ASC ");
    }

    public long getGuaranteedBalanceATM() {
        return getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), blockchain.getHeight());
    }

    public long getGuaranteedBalanceATM(final int numberOfConfirmations, final int currentHeight) {
        sync.readLock();
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
            sync.readUnlock();
        }
    }

    public DbIterator<AccountAsset> getAssets(int from, int to) {
        return AccountAssetTable.getInstance().getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountAsset> getAssets(int height, int from, int to) {
        return AccountAssetTable.getInstance().getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
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
        return AccountAssetTable.getInstance().get(AccountAssetTable.newKey(this.id, assetId));
    }

    public AccountAsset getAsset(long assetId, int height) {
        return AccountAssetTable.getInstance().get(AccountAssetTable.newKey(this.id, assetId), height);
    }

    public long getAssetBalanceATU(long assetId) {
        return AccountAssetTable.getAssetBalanceATU(this.id, assetId);
    }

    public long getAssetBalanceATU(long assetId, int height) {
        return AccountAssetTable.getAssetBalanceATU(this.id, assetId, height);
    }

    public long getUnconfirmedAssetBalanceATU(long assetId) {
        return AccountAssetTable.getUnconfirmedAssetBalanceATU(this.id, assetId);
    }

    public AccountCurrency getCurrency(long currencyId) {
        return AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(this.id, currencyId));
    }

    public AccountCurrency getCurrency(long currencyId, int height) {
        return AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(this.id, currencyId), height);
    }

    public DbIterator<AccountCurrency> getCurrencies(int from, int to) {
        return AccountCurrencyTable.getInstance().getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountCurrency> getCurrencies(int height, int from, int to) {
        return AccountCurrencyTable.getInstance().getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public long getCurrencyUnits(long currencyId) {
        return AccountCurrencyTable.getCurrencyUnits(this.id, currencyId);
    }

    public long getCurrencyUnits(long currencyId, int height) {
        return AccountCurrencyTable.getCurrencyUnits(this.id, currencyId, height);
    }

    public long getUnconfirmedCurrencyUnits(long currencyId) {
        return AccountCurrencyTable.getUnconfirmedCurrencyUnits(this.id, currencyId);
    }

    public Set<ControlType> getControls() {
        return controls;
    }

    public void leaseEffectiveBalance(long lesseeId, int period) {
        int height = blockchain.getHeight();
        AccountLease accountLease = AccountLeaseTable.getInstance().get(AccountTable.newKey(this));
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
        AccountLeaseTable.getInstance().insert(accountLease);
        leaseListeners.notify(accountLease, Event.LEASE_SCHEDULED);
    }

    public void addControl(ControlType control) {
        if (controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        AccountTable.getInstance().insert(this);
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
        AccountProperty accountProperty = AccountPropertyTable.getProperty(this.id, property, setterAccount.id);
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), this.id, setterAccount.id, property, value);
        } else {
            accountProperty.value = value;
        }
        AccountPropertyTable.getInstance().insert(accountProperty);
        listeners.notify(this, Event.SET_PROPERTY);
        propertyListeners.notify(accountProperty, Event.SET_PROPERTY);
    }

   public  void deleteProperty(long propertyId) {
        AccountProperty accountProperty = AccountPropertyTable.getInstance().get(AccountPropertyTable.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != this.id && accountProperty.getRecipientId() != this.id) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(this.id));
        }
        AccountPropertyTable.getInstance().delete(accountProperty);
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
                GenesisPublicKeyTable.getInstance().insert(publicKey);
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
        accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, 0);
        } else {
            accountAsset.quantityATU = assetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
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
        accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(this.id, assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
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
        accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(this.id, assetId));
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
        AccountAssetTable.getInstance().save(accountAsset);
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
        accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(this.id, currencyId));
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
        AccountCurrency accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(this.id, currencyId));
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
        accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(this.id, currencyId));
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
        try (DbIterator<AccountAsset> iterator = AccountAssetTable.getAssetAccounts(attachment.getAssetId(), attachment.getHeight(), 0, -1)) {
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
