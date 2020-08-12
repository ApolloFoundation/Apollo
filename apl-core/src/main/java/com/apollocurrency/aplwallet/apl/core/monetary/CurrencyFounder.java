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

package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
/**
 * Each CurrencyFounder instance represents a single founder contribution for a non issued currency
 * Once the currency is issued all founder contributions are removed
 * In case the currency is not issued because of insufficient funding, all funds are returned to the founders
 */
public class CurrencyFounder {
    private static final BlockChainInfoService BLOCK_CHAIN_INFO_SERVICE =
        CDI.current().select(BlockChainInfoService.class).get();

    /**
     * @deprecated
     */
    private static final LinkKeyFactory<CurrencyFounder> currencyFounderDbKeyFactory = new LinkKeyFactory<CurrencyFounder>("currency_id", "account_id") {

        @Override
        public DbKey newKey(CurrencyFounder currencyFounder) {
            return currencyFounder.dbKey;
        }

    };

    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<CurrencyFounder> currencyFounderTable = new VersionedDeletableEntityDbTable<CurrencyFounder>("currency_founder", currencyFounderDbKeyFactory) {

        @Override
        public CurrencyFounder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencyFounder(rs, dbKey);
        }

        @Override
        public void save(Connection con, CurrencyFounder currencyFounder) throws SQLException {
            currencyFounder.save(con);
        }

        @Override
        public String defaultSort() {
            return " ORDER BY height DESC ";
        }

    };
    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private long amountPerUnitATM;

    private CurrencyFounder(long currencyId, long accountId, long amountPerUnitATM) {
        this.currencyId = currencyId;
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId, accountId);
        this.accountId = accountId;
        this.amountPerUnitATM = amountPerUnitATM;
    }

    private CurrencyFounder(ResultSet rs, DbKey dbKey) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.amountPerUnitATM = rs.getLong("amount");
    }

    /**
     * @deprecated
     */
    public static void init() {
    }

    /**
     * @deprecated
     */
    static void addOrUpdateFounder(long currencyId, long accountId, long amount) {
        CurrencyFounder founder = getFounder(currencyId, accountId);
        if (founder == null) {
            founder = new CurrencyFounder(currencyId, accountId, amount);
        } else {
            founder.amountPerUnitATM += amount;
        }
        currencyFounderTable.insert(founder);
    }

    /**
     * @deprecated
     */
    public static CurrencyFounder getFounder(long currencyId, long accountId) {
        return currencyFounderTable.get(currencyFounderDbKeyFactory.newKey(currencyId, accountId));
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<CurrencyFounder> getFounderCurrencies(long accountId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    /**
     * @deprecated
     */
    static void remove(long currencyId) {
        List<CurrencyFounder> founders = new ArrayList<>();
        try (DbIterator<CurrencyFounder> currencyFounders = CurrencyFounder.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : currencyFounders) {
                founders.add(founder);
            }
        }
        founders.forEach(f -> currencyFounderTable.deleteAtHeight(f, BLOCK_CHAIN_INFO_SERVICE.getHeight()));
    }

    @Override
    public String toString() {
        return "CurrencyFounder{" +
            "dbKey=" + dbKey +
            ", currencyId=" + currencyId +
            ", accountId=" + accountId +
            ", amountPerUnitATM=" + amountPerUnitATM +
            '}';
    }

    private void save(Connection con) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_founder (currency_id, account_id, amount, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE currency_id = VALUES(currency_id), account_id = VALUES(account_id), amount = VALUES(amount), "
                + "height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getAmountPerUnitATM());
            pstmt.setInt(++i, BLOCK_CHAIN_INFO_SERVICE.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAmountPerUnitATM() {
        return amountPerUnitATM;
    }
}
