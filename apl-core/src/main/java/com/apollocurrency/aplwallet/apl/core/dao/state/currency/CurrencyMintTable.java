/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

public class CurrencyMintTable extends VersionedDeletableEntityDbTable<CurrencyMint> {

    public static final LinkKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new LinkKeyFactory<>("currency_id", "account_id") {
        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            if (currencyMint.getDbKey() == null) {
                currencyMint.setDbKey(super.newKey(currencyMint.getCurrencyId(), currencyMint.getAccountId()));
            }
            return currencyMint.getDbKey();
        }
    };

    public CurrencyMintTable() {
        super("currency_mint", currencyMintDbKeyFactory);
    }

    @Override
    public CurrencyMint load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyMint(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencyMint currencyMint) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_mint (currency_id, account_id, counter, height, latest, deleted) "
                + "KEY (currency_id, account_id, height) VALUES (?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, currencyMint.getCurrencyId());
            pstmt.setLong(++i, currencyMint.getAccountId());
            pstmt.setLong(++i, currencyMint.getCounter());
            pstmt.setInt(++i, currencyMint.getHeight());
            pstmt.executeUpdate();
        }
    }

}
