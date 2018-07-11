/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.AccountLedger.LedgerEntry;
import com.apollocurrency.aplwallet.apl.AccountLedger.LedgerEvent;
import com.apollocurrency.aplwallet.apl.AccountLedger.LedgerHolding;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.db.DbClause;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbKey;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import com.apollocurrency.aplwallet.apl.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.db.VersionedEntityDbTable;
import com.apollocurrency.aplwallet.apl.db.VersionedPersistentDbTable;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.Logger;

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

@SuppressWarnings({"UnusedDeclaration", "SuspiciousNameCombination"})
public final class Account {

    public enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE, CURRENCY_BALANCE, UNCONFIRMED_CURRENCY_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED, SET_PROPERTY, DELETE_PROPERTY
    }

    public enum ControlType {
        PHASING_ONLY
    }

    public static final class AccountAsset {

        private final long accountId;
        private final long assetId;
        private final DbKey dbKey;
        private long quantityATU;
        private long unconfirmedQuantityATU;

        private AccountAsset(long accountId, long assetId, long quantityATU, long unconfirmedQuantityATU) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityATU = quantityATU;
            this.unconfirmedQuantityATU = unconfirmedQuantityATU;
        }

        private AccountAsset(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.assetId = rs.getLong("asset_id");
            this.dbKey = dbKey;
            this.quantityATU = rs.getLong("quantity");
            this.unconfirmedQuantityATU = rs.getLong("unconfirmed_quantity");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.assetId);
                pstmt.setLong(++i, this.quantityATU);
                pstmt.setLong(++i, this.unconfirmedQuantityATU);
                pstmt.setInt(++i, Apl.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityATU() {
            return quantityATU;
        }

        public long getUnconfirmedQuantityATU() {
            return unconfirmedQuantityATU;
        }

        private void save() {
            checkBalance(this.accountId, this.quantityATU, this.unconfirmedQuantityATU);
            if (this.quantityATU > 0 || this.unconfirmedQuantityATU > 0) {
                accountAssetTable.insert(this);
            } else {
                accountAssetTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: " + Long.toUnsignedString(accountId) + " asset_id: " + Long.toUnsignedString(assetId)
                    + " quantity: " + quantityATU + " unconfirmedQuantity: " + unconfirmedQuantityATU;
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    public static final class AccountCurrency {

        private final long accountId;
        private final long currencyId;
        private final DbKey dbKey;
        private long units;
        private long unconfirmedUnits;

        private AccountCurrency(long accountId, long currencyId, long quantityATU, long unconfirmedQuantityATU) {
            this.accountId = accountId;
            this.currencyId = currencyId;
            this.dbKey = accountCurrencyDbKeyFactory.newKey(this.accountId, this.currencyId);
            this.units = quantityATU;
            this.unconfirmedUnits = unconfirmedQuantityATU;
        }

        private AccountCurrency(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.currencyId = rs.getLong("currency_id");
            this.dbKey = dbKey;
            this.units = rs.getLong("units");
            this.unconfirmedUnits = rs.getLong("unconfirmed_units");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_currency "
                    + "(account_id, currency_id, units, unconfirmed_units, height, latest) "
                    + "KEY (account_id, currency_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.currencyId);
                pstmt.setLong(++i, this.units);
                pstmt.setLong(++i, this.unconfirmedUnits);
                pstmt.setInt(++i, Apl.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getUnconfirmedUnits() {
            return unconfirmedUnits;
        }

        private void save() {
            checkBalance(this.accountId, this.units, this.unconfirmedUnits);
            if (this.units > 0 || this.unconfirmedUnits > 0) {
                accountCurrencyTable.insert(this);
            } else if (this.units == 0 && this.unconfirmedUnits == 0) {
                accountCurrencyTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountCurrency account_id: " + Long.toUnsignedString(accountId) + " currency_id: " + Long.toUnsignedString(currencyId)
                    + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
        }

    }

    public static final class AccountLease {

        private final long lessorId;
        private final DbKey dbKey;
        private long currentLesseeId;
        private int currentLeasingHeightFrom;
        private int currentLeasingHeightTo;
        private long nextLesseeId;
        private int nextLeasingHeightFrom;
        private int nextLeasingHeightTo;

        private AccountLease(long lessorId,
                             int currentLeasingHeightFrom, int currentLeasingHeightTo, long currentLesseeId) {
            this.lessorId = lessorId;
            this.dbKey = accountLeaseDbKeyFactory.newKey(this.lessorId);
            this.currentLeasingHeightFrom = currentLeasingHeightFrom;
            this.currentLeasingHeightTo = currentLeasingHeightTo;
            this.currentLesseeId = currentLesseeId;
        }

        private AccountLease(ResultSet rs, DbKey dbKey) throws SQLException {
            this.lessorId = rs.getLong("lessor_id");
            this.dbKey = dbKey;
            this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
            this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
            this.currentLesseeId = rs.getLong("current_lessee_id");
            this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
            this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
            this.nextLesseeId = rs.getLong("next_lessee_id");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease "
                    + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, "
                    + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) "
                    + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.lessorId);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightFrom);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightTo);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.currentLesseeId);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightFrom);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightTo);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.nextLesseeId);
                pstmt.setInt(++i, Apl.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getLessorId() {
            return lessorId;
        }

        public long getCurrentLesseeId() {
            return currentLesseeId;
        }

        public int getCurrentLeasingHeightFrom() {
            return currentLeasingHeightFrom;
        }

        public int getCurrentLeasingHeightTo() {
            return currentLeasingHeightTo;
        }

        public long getNextLesseeId() {
            return nextLesseeId;
        }

        public int getNextLeasingHeightFrom() {
            return nextLeasingHeightFrom;
        }

        public int getNextLeasingHeightTo() {
            return nextLeasingHeightTo;
        }

    }

    public static final class AccountInfo {

        private final long accountId;
        private final DbKey dbKey;
        private String name;
        private String description;

        private AccountInfo(long accountId, String name, String description) {
            this.accountId = accountId;
            this.dbKey = accountInfoDbKeyFactory.newKey(this.accountId);
            this.name = name;
            this.description = description;
        }

        private AccountInfo(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.dbKey = dbKey;
            this.name = rs.getString("name");
            this.description = rs.getString("description");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_info "
                    + "(account_id, name, description, height, latest) "
                    + "KEY (account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                DbUtils.setString(pstmt, ++i, this.name);
                DbUtils.setString(pstmt, ++i, this.description);
                pstmt.setInt(++i, Apl.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        private void save() {
            if (this.name != null || this.description != null) {
                accountInfoTable.insert(this);
            } else {
                accountInfoTable.delete(this);
            }
        }

    }

    public static final class AccountProperty {

        private final long id;
        private final DbKey dbKey;
        private final long recipientId;
        private final long setterId;
        private String property;
        private String value;

        private AccountProperty(long id, long recipientId, long setterId, String property, String value) {
            this.id = id;
            this.dbKey = accountPropertyDbKeyFactory.newKey(this.id);
            this.recipientId = recipientId;
            this.setterId = setterId;
            this.property = property;
            this.value = value;
        }

        private AccountProperty(ResultSet rs, DbKey dbKey) throws SQLException {
            this.id = rs.getLong("id");
            this.dbKey = dbKey;
            this.recipientId = rs.getLong("recipient_id");
            long setterId = rs.getLong("setter_id");
            this.setterId = setterId == 0 ? recipientId : setterId;
            this.property = rs.getString("property");
            this.value = rs.getString("value");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property "
                    + "(id, recipient_id, setter_id, property, value, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.id);
                pstmt.setLong(++i, this.recipientId);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.setterId != this.recipientId ? this.setterId : 0);
                DbUtils.setString(pstmt, ++i, this.property);
                DbUtils.setString(pstmt, ++i, this.value);
                pstmt.setInt(++i, Apl.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getId() {
            return id;
        }

        public long getRecipientId() {
            return recipientId;
        }

        public long getSetterId() {
            return setterId;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

    }

    public static final class PublicKey {

        private final long accountId;
        private final DbKey dbKey;
        private byte[] publicKey;
        private int height;

        private PublicKey(long accountId, byte[] publicKey) {
            this.accountId = accountId;
            this.dbKey = publicKeyDbKeyFactory.newKey(accountId);
            this.publicKey = publicKey;
            this.height = Apl.getBlockchain().getHeight();
        }

        private PublicKey(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.dbKey = dbKey;
            this.publicKey = rs.getBytes("public_key");
            this.height = rs.getInt("height");
        }

        private void save(Connection con) throws SQLException {
            height = Apl.getBlockchain().getHeight();
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO public_key (account_id, public_key, height, latest) "
                    + "KEY (account_id, height) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, accountId);
                DbUtils.setBytes(pstmt, ++i, publicKey);
                pstmt.setInt(++i, height);
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public int getHeight() {
            return height;
        }

    }

    static class DoubleSpendingException extends RuntimeException {

        DoubleSpendingException(String message, long accountId, long confirmed, long unconfirmed) {
            super(message + " account: " + Long.toUnsignedString(accountId) + " confirmed: " + confirmed + " unconfirmed: " + unconfirmed);
        }

    }

    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey == null ? newKey(account.id) : account.dbKey;
        }

        @Override
        public Account newEntity(DbKey dbKey) {
            return new Account(((DbKey.LongKey)dbKey).getId());
        }

    };

    private static final VersionedEntityDbTable<Account> accountTable = new VersionedEntityDbTable<Account>("account", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Account(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }

        @Override
        public void trim(int height) {
            if (height <= Constants.GUARANTEED_BALANCE_CONFIRMATIONS) {
                return;
            }
            super.trim(height);
        }
        @Override
        public void checkAvailable(int height) {
            if (height > Constants.GUARANTEED_BALANCE_CONFIRMATIONS) {
                super.checkAvailable(height);
                return;
            }
            if (height > Apl.getBlockchain().getHeight()) {
                throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Apl.getBlockchain().getHeight());
            }
        }

    };

    private static final DbKey.LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new DbKey.LongKeyFactory<AccountInfo>("account_id") {

        @Override
        public DbKey newKey(AccountInfo accountInfo) {
            return accountInfo.dbKey;
        }

    };

    private static final DbKey.LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new DbKey.LongKeyFactory<AccountLease>("lessor_id") {

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

    private static final VersionedEntityDbTable<AccountInfo> accountInfoTable = new VersionedEntityDbTable<AccountInfo>("account_info",
            accountInfoDbKeyFactory, "name,description") {

        @Override
        protected AccountInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountInfo(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountInfo accountInfo) throws SQLException {
            accountInfo.save(con);
        }

    };

    private static final DbKey.LongKeyFactory<PublicKey> publicKeyDbKeyFactory = new DbKey.LongKeyFactory<PublicKey>("account_id") {

        @Override
        public DbKey newKey(PublicKey publicKey) {
            return publicKey.dbKey;
        }

        @Override
        public PublicKey newEntity(DbKey dbKey) {
            return new PublicKey(((DbKey.LongKey)dbKey).getId(), null);
        }

    };

    private static final VersionedPersistentDbTable<PublicKey> publicKeyTable = new VersionedPersistentDbTable<PublicKey>("public_key", publicKeyDbKeyFactory) {

        @Override
        protected PublicKey load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PublicKey(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PublicKey publicKey) throws SQLException {
            publicKey.save(con);
        }

    };

    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<AccountAsset>("account_id", "asset_id") {

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new VersionedEntityDbTable<AccountAsset>("account_asset", accountAssetDbKeyFactory) {

        @Override
        protected AccountAsset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountAsset(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
            accountAsset.save(con);
        }

        @Override
        public void trim(int height) {
            super.trim(Math.max(0, height - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK));
        }

        @Override
        public void checkAvailable(int height) {
            if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < Apl.getBlockchainProcessor().getMinRollbackHeight()) {
                throw new IllegalArgumentException("Historical data as of height " + height +" not available.");
            }
            if (height > Apl.getBlockchain().getHeight()) {
                throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Apl.getBlockchain().getHeight());
            }
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY quantity DESC, account_id, asset_id ";
        }

    };

    private static final DbKey.LinkKeyFactory<AccountCurrency> accountCurrencyDbKeyFactory = new DbKey.LinkKeyFactory<AccountCurrency>("account_id", "currency_id") {

        @Override
        public DbKey newKey(AccountCurrency accountCurrency) {
            return accountCurrency.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountCurrency> accountCurrencyTable = new VersionedEntityDbTable<AccountCurrency>("account_currency", accountCurrencyDbKeyFactory) {

        @Override
        protected AccountCurrency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountCurrency(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
            accountCurrency.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY units DESC, account_id, currency_id ";
        }

    };

    private static final DerivedDbTable accountGuaranteedBalanceTable = new DerivedDbTable("account_guaranteed_balance") {

        @Override
        public void trim(int height) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance "
                         + "WHERE height < ? AND height >= 0 LIMIT " + Constants.BATCH_COMMIT_SIZE)) {
                pstmtDelete.setInt(1, height - Constants.GUARANTEED_BALANCE_CONFIRMATIONS);
                int count;
                do {
                    count = pstmtDelete.executeUpdate();
                    Db.db.commitTransaction();
                } while (count >= Constants.BATCH_COMMIT_SIZE);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

    };

    private static final DbKey.LongKeyFactory<AccountProperty> accountPropertyDbKeyFactory = new DbKey.LongKeyFactory<AccountProperty>("id") {

        @Override
        public DbKey newKey(AccountProperty accountProperty) {
            return accountProperty.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountProperty> accountPropertyTable = new VersionedEntityDbTable<AccountProperty>("account_property", accountPropertyDbKeyFactory) {

        @Override
        protected AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountProperty(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountProperty accountProperty) throws SQLException {
            accountProperty.save(con);
        }

    };

    private static final ConcurrentMap<DbKey, byte[]> publicKeyCache = Apl.getBooleanProperty("apl.enablePublicKeyCache") ?
            new ConcurrentHashMap<>() : null;

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    private static final Listeners<AccountCurrency,Event> currencyListeners = new Listeners<>();

    private static final Listeners<AccountLease,Event> leaseListeners = new Listeners<>();

    private static final Listeners<AccountProperty,Event> propertyListeners = new Listeners<>();

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
        return publicKeyTable.getCount();
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
        return accountPropertyTable.get(accountPropertyDbKeyFactory.newKey(propertyId));
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
            PublicKey publicKey = publicKeyTable.get(dbKey);
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
            PublicKey publicKey = publicKeyTable.get(dbKey, height);
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
            account.publicKey = publicKeyTable.get(accountDbKeyFactory.newKey(account));
        }
        if (account.publicKey == null || account.publicKey.publicKey == null || Arrays.equals(account.publicKey.publicKey, publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(accountId)
                + " existing key " + Convert.toHexString(account.publicKey.publicKey) + " new key " + Convert.toHexString(publicKey));
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
            PublicKey publicKey = publicKeyTable.get(dbKey);
            if (publicKey == null || (key = publicKey.publicKey) == null) {
                return null;
            }
            if (publicKeyCache != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
        return key;
    }

    static Account addOrGetAccount(long id) {
        if (id == 0) {
            throw new IllegalArgumentException("Invalid accountId 0");
        }
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            account = accountTable.newEntity(dbKey);
            PublicKey publicKey = publicKeyTable.get(dbKey);
            if (publicKey == null) {
                publicKey = publicKeyTable.newEntity(dbKey);
                publicKeyTable.insert(publicKey);
            }
            account.publicKey = publicKey;
        }
        return account;
    }

    private static DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
                            + "ORDER BY current_lessee_id, lessor_id");
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            return accountLeaseTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
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
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId, int height) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId, int height) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
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
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getUnconfirmedAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
    }

    public static long getCurrencyUnits(long accountId, long currencyId, int height) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getUnconfirmedCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
    }

    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
    }

    static {

        Apl.getBlockchainProcessor().addListener(block -> {
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

            Apl.getBlockchainProcessor().addListener(block -> {
                publicKeyCache.remove(accountDbKeyFactory.newKey(block.getGeneratorId()));
                block.getTransactions().forEach(transaction -> {
                    publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getSenderId()));
                    if (!transaction.getAppendages(appendix -> (appendix instanceof Appendix.PublicKeyAnnouncement), false).isEmpty()) {
                        publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getRecipientId()));
                    }
                    if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                        Attachment.ShufflingRecipients shufflingRecipients = (Attachment.ShufflingRecipients) transaction.getAttachment();
                        for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                            publicKeyCache.remove(accountDbKeyFactory.newKey(Account.getId(publicKey)));
                        }
                    }
                });
            }, BlockchainProcessor.Event.BLOCK_POPPED);

            Apl.getBlockchainProcessor().addListener(block -> publicKeyCache.clear(), BlockchainProcessor.Event.RESCAN_BEGIN);

        }

    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private PublicKey publicKey;
    private long balanceATM;
    private long unconfirmedBalanceATM;
    private long forgedBalanceATM;
    private long activeLesseeId;
    private Set<ControlType> controls;

    private Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.controls = Collections.emptySet();
    }

    private Account(ResultSet rs, DbKey dbKey) throws SQLException {
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

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, "
                + "balance, unconfirmed_balance, forged_balance, "
                + "active_lessee_id, has_control_phasing, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.balanceATM);
            pstmt.setLong(++i, this.unconfirmedBalanceATM);
            pstmt.setLong(++i, this.forgedBalanceATM);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.activeLesseeId);
            pstmt.setBoolean(++i, controls.contains(ControlType.PHASING_ONLY));
            pstmt.setInt(++i, Apl.getBlockchain().getHeight());
            pstmt.executeUpdate();
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

    void setAccountInfo(String name, String description) {
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

    public EncryptedData encryptTo(byte[] data, String senderSecretPhrase, boolean compress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return Account.encryptTo(key, data, senderSecretPhrase, compress);
    }

    public static EncryptedData encryptTo(byte[] publicKey, byte[] data, String senderSecretPhrase, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, senderSecretPhrase, publicKey);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, String recipientSecretPhrase, boolean uncompress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return Account.decryptFrom(key, encryptedData, recipientSecretPhrase, uncompress);
    }

    public static byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, String recipientSecretPhrase, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientSecretPhrase, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
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
        return getEffectiveBalanceAPL(Apl.getBlockchain().getHeight());
    }

    public long getEffectiveBalanceAPL(int height) {
        if (height <= 1440) {
            Account genesisAccount = getAccount(id, 0);
            return genesisAccount == null ? 0 : genesisAccount.getBalanceATM() / Constants.ONE_APL;
        }
        if (this.publicKey == null) {
            this.publicKey = publicKeyTable.get(accountDbKeyFactory.newKey(this));
        }
        if (this.publicKey == null || this.publicKey.publicKey == null || height - this.publicKey.height <= 1440) {
            return 0; // cfb: Accounts with the public key revealed less than 1440 blocks ago are not allowed to generate blocks
        }
        Apl.getBlockchain().readLock();
        try {
            long effectiveBalanceATM = getLessorsGuaranteedBalanceATM(height);
            if (activeLesseeId == 0) {
                effectiveBalanceATM += getGuaranteedBalanceATM(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, height);
            }
	        return effectiveBalanceATM < Constants.MIN_FORGING_BALANCE_ATM ? 0 : effectiveBalanceATM / Constants.ONE_APL;
        } finally {
            Apl.getBlockchain().readUnlock();
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
        int blockchainHeight = Apl.getBlockchain().getHeight();
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS additions "
                     + "FROM account_guaranteed_balance, TABLE (id BIGINT=?) T WHERE account_id = T.id AND height > ? "
                     + (height < blockchainHeight ? " AND height <= ? " : "")
                     + " GROUP BY account_id ORDER BY account_id")) {
            pstmt.setObject(1, lessorIds);
            pstmt.setInt(2, height - Constants.GUARANTEED_BALANCE_CONFIRMATIONS);
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
        } catch (SQLException e) {
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
        return getGuaranteedBalanceATM(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, Apl.getBlockchain().getHeight());
    }

    public long getGuaranteedBalanceATM(final int numberOfConfirmations, final int currentHeight) {
        Apl.getBlockchain().readLock();
        try {
            int height = currentHeight - numberOfConfirmations;
            if (height + Constants.GUARANTEED_BALANCE_CONFIRMATIONS < Apl.getBlockchainProcessor().getMinRollbackHeight()
                    || height > Apl.getBlockchain().getHeight()) {
                throw new IllegalArgumentException("Height " + height + " not available for guaranteed balance calculation");
            }
            try (Connection con = Db.db.getConnection();
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
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        } finally {
            Apl.getBlockchain().readUnlock();
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
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
    }

    public AccountAsset getAsset(long assetId, int height) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId), height);
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
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
    }

    public AccountCurrency getCurrency(long currencyId, int height) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId), height);
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

    void leaseEffectiveBalance(long lesseeId, int period) {
        int height = Apl.getBlockchain().getHeight();
        AccountLease accountLease = accountLeaseTable.get(accountDbKeyFactory.newKey(this));
        if (accountLease == null) {
            accountLease = new AccountLease(id,
                    height + Constants.LEASING_DELAY,
                    height + Constants.LEASING_DELAY + period,
                    lesseeId);
        } else if (accountLease.currentLesseeId == 0) {
            accountLease.currentLeasingHeightFrom = height + Constants.LEASING_DELAY;
            accountLease.currentLeasingHeightTo = height + Constants.LEASING_DELAY + period;
            accountLease.currentLesseeId = lesseeId;
        } else {
            accountLease.nextLeasingHeightFrom = height + Constants.LEASING_DELAY;
            if (accountLease.nextLeasingHeightFrom < accountLease.currentLeasingHeightTo) {
                accountLease.nextLeasingHeightFrom = accountLease.currentLeasingHeightTo;
            }
            accountLease.nextLeasingHeightTo = accountLease.nextLeasingHeightFrom + period;
            accountLease.nextLesseeId = lesseeId;
        }
        accountLeaseTable.insert(accountLease);
        leaseListeners.notify(accountLease, Event.LEASE_SCHEDULED);
    }

    void addControl(ControlType control) {
        if (controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        accountTable.insert(this);
    }

    void removeControl(ControlType control) {
        if (!controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.copyOf(controls);
        newControls.remove(control);
        controls = Collections.unmodifiableSet(newControls);
        save();
    }

    void setProperty(Transaction transaction, Account setterAccount, String property, String value) {
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

    void deleteProperty(long propertyId) {
        AccountProperty accountProperty = accountPropertyTable.get(accountPropertyDbKeyFactory.newKey(propertyId));
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

    static boolean setOrVerify(long accountId, byte[] key) {
        DbKey dbKey = publicKeyDbKeyFactory.newKey(accountId);
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKey.height = Apl.getBlockchain().getHeight();
            return true;
        }
        return Arrays.equals(publicKey.publicKey, key);
    }

    void apply(byte[] key) {
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKeyTable.insert(publicKey);
        } else if (! Arrays.equals(publicKey.publicKey, key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.height >= Apl.getBlockchain().getHeight() - 1) {
            PublicKey dbPublicKey = publicKeyTable.get(dbKey, false);
            if (dbPublicKey == null || dbPublicKey.publicKey == null) {
                publicKeyTable.insert(publicKey);
            }
        }
        if (publicKeyCache != null) {
            publicKeyCache.put(dbKey, key);
        }
        this.publicKey = publicKey;
    }

    void addToAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
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

    void addToUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
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

    void addToAssetAndUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
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

    void addToCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
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

    void addToUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
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

    void addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
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

    void addToBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToBalanceATM(event, eventId, amountATM, 0);
    }

    void addToBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
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

    void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToUnconfirmedBalanceATM(event, eventId, amountATM, 0);
    }

    void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
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

    void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
        addToBalanceAndUnconfirmedBalanceATM(event, eventId, amountATM, 0);
    }

    void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
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

    void addToForgedBalanceATM(long amountATM) {
        if (amountATM == 0) {
            return;
        }
        this.forgedBalanceATM = Math.addExact(this.forgedBalanceATM, amountATM);
        save();
    }

    private static void checkBalance(long accountId, long confirmed, long unconfirmed) {
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

    private void addToGuaranteedBalanceATM(long amountATM) {
        if (amountATM <= 0) {
            return;
        }
        int blockchainHeight = Apl.getBlockchain().getHeight();
        try (Connection con = Db.db.getConnection();
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
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    void payDividends(final long transactionId, Attachment.ColoredCoinsDividendPayment attachment) {
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
