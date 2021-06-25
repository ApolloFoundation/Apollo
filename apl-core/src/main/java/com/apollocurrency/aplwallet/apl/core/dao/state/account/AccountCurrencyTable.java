/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author al
 */
@Singleton
public class AccountCurrencyTable extends VersionedDeletableEntityDbTable<AccountCurrency> {

    private static final LinkKeyFactory<AccountCurrency> accountCurrencyDbKeyFactory = new LinkKeyFactory<>("account_id", "currency_id") {
        @Override
        public DbKey newKey(AccountCurrency accountCurrency) {
            if (accountCurrency.getDbKey() == null) {
                accountCurrency.setDbKey(super.newKey(accountCurrency.getAccountId(), accountCurrency.getCurrencyId()));
            }
            return accountCurrency.getDbKey();
        }
    };

    @Inject
    public AccountCurrencyTable(DatabaseManager databaseManager,
                                Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("account_currency", accountCurrencyDbKeyFactory, null,
                databaseManager, deleteOnTrimDataEvent);
    }

    public static DbKey newKey(long idA, long idB) {
        return accountCurrencyDbKeyFactory.newKey(idA, idB);
    }

    @Override
    public AccountCurrency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountCurrency(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_currency "
                + "(account_id, currency_id, units, unconfirmed_units, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE "
                + "account_id = VALUES(account_id), currency_id = VALUES(currency_id), units = VALUES(units), "
                + "unconfirmed_units = VALUES(unconfirmed_units), height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, accountCurrency.getAccountId());
            pstmt.setLong(++i, accountCurrency.getCurrencyId());
            pstmt.setLong(++i, accountCurrency.getUnits());
            pstmt.setLong(++i, accountCurrency.getUnconfirmedUnits());
            pstmt.setInt(++i, accountCurrency.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY units DESC, account_id, currency_id ";
    }

    public DbIterator<AccountCurrency> getByAccount(long accountId, int from, int to) {
        return getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public DbIterator<AccountCurrency> getByAccount(long accountId, int height, int from, int to) {
        return getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public DbIterator<AccountCurrency> getByCurrency(long currencyId, int from, int to) {
        return getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public DbIterator<AccountCurrency> getByCurrency(long currencyId, int height, int from, int to) {
        return getManyBy(new DbClause.LongClause("currency_id", currencyId), height, from, to);
    }

    public int getCountByCurrency(long currencyId) {
        return getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    public int getCountByCurrency(long currencyId, int height) {
        return getCount(new DbClause.LongClause("currency_id", currencyId), height);
    }

    public int getCountByAccount(long accountId) {
        return getCount(new DbClause.LongClause("account_id", accountId));
    }

    public int getCountByAccount(long accountId, int height) {
        return getCount(new DbClause.LongClause("account_id", accountId), height);
    }
}
