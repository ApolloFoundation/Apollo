/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class CurrencyMint extends VersionedDeletableEntity {

    private long currencyId;
    private long accountId;
    private long counter;

    public CurrencyMint(long currencyId, long accountId, long counter, int height) {
        super(null, height);
        this.currencyId = currencyId;
        this.accountId = accountId;
        super.setDbKey(CurrencyMintTable.currencyMintDbKeyFactory.newKey(this.currencyId, this.accountId));
        this.counter = counter;
    }

    /**
     * unit test
     */
    public CurrencyMint(long currencyId, long accountId, long counter, int height, boolean latest, boolean deleted) {
        super(null, height);
        this.currencyId = currencyId;
        this.accountId = accountId;
        this.counter = counter;
        super.setLatest(latest);
        super.setDeleted(deleted);
    }

    public CurrencyMint(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        super.setDbKey(dbKey);
        this.counter = rs.getLong("counter");
    }
}
