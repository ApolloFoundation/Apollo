/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyFounderTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class CurrencyFounder extends VersionedDeletableEntity {

    private long currencyId;
    private long accountId;
    private long amountPerUnitATM;

    public CurrencyFounder(long currencyId, long accountId, long amountPerUnitATM, int height) {
        super(null, height);
        this.currencyId = currencyId;
        super.setDbKey(CurrencyFounderTable.currencyFounderDbKeyFactory.newKey(currencyId, accountId));
        this.accountId = accountId;
        this.amountPerUnitATM = amountPerUnitATM;
    }

    /**
     * for unit test
     */
    public CurrencyFounder(long currencyId, long accountId, long amountPerUnitATM, int height, boolean latest, boolean deleted) {
        super(null, height);
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.amountPerUnitATM = amountPerUnitATM;
        this.setLatest(latest);
        this.setDeleted(deleted);
    }

    public CurrencyFounder(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        super.setDbKey(dbKey);
        this.amountPerUnitATM = rs.getLong("amount");
    }


}
