/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@ToString(callSuper = true)
@Getter @Setter
public final class AccountCurrency extends VersionedDerivedEntity {

    final long accountId;
    final long currencyId;
    long units;
    long unconfirmedUnits;

    public AccountCurrency(long accountId, long currencyId, long quantityATU, long unconfirmedQuantityATU, int height) {
        super(null, height);
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.units = quantityATU;
        this.unconfirmedUnits = unconfirmedQuantityATU;
    }

    public AccountCurrency(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.accountId = rs.getLong("account_id");
        this.currencyId = rs.getLong("currency_id");
        this.units = rs.getLong("units");
        this.unconfirmedUnits = rs.getLong("unconfirmed_units");
        setDbKey(dbKey);
    }

}
