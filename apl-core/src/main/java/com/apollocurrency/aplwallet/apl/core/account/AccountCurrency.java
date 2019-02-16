/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@SuppressWarnings(value = "UnusedDeclaration")
public final class AccountCurrency {
    
    final long accountId;
    final long currencyId;
    final DbKey dbKey;
    long units;
    long unconfirmedUnits;
    
    public AccountCurrency(long accountId, long currencyId, long quantityATU, long unconfirmedQuantityATU) {
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.dbKey = AccountCurrencyTable.newKey(this.accountId, this.currencyId);
        this.units = quantityATU;
        this.unconfirmedUnits = unconfirmedQuantityATU;
    }

    public AccountCurrency(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.currencyId = rs.getLong("currency_id");
        this.dbKey = dbKey;
        this.units = rs.getLong("units");
        this.unconfirmedUnits = rs.getLong("unconfirmed_units");
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

    void save() {
        Account.checkBalance(this.accountId, this.units, this.unconfirmedUnits);
        if (this.units > 0 || this.unconfirmedUnits > 0) {
            AccountCurrencyTable.getInstance().insert(this);
        } else if (this.units == 0 && this.unconfirmedUnits == 0) {
            AccountCurrencyTable.getInstance().delete(this);
        }
    }

    @Override
    public String toString() {
        return "AccountCurrency account_id: " + Long.toUnsignedString(accountId) + " currency_id: " + Long.toUnsignedString(currencyId) + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
    }
    
}
