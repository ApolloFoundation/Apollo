/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@SuppressWarnings(value = "UnusedDeclaration")
@Getter @Setter
public final class AccountCurrency {
    //TODO remove the unneeded public scope
    public final long accountId;
    public final long currencyId;
    public final DbKey dbKey;
    public long units;
    public long unconfirmedUnits;
    
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

    @Override
    public String toString() {
        return "AccountCurrency account_id: " + Long.toUnsignedString(accountId) + " currency_id: " + Long.toUnsignedString(currencyId) + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
    }
    
}
