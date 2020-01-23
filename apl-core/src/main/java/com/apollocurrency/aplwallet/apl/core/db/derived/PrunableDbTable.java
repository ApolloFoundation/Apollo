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

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class PrunableDbTable<T> extends EntityDbTable<T> {
    private static final Logger LOG = getLogger(PrunableDbTable.class);
    private final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    public  PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    protected PrunableDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    public PrunableDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns, boolean init) {
        super(table, dbKeyFactory, multiversion, fullTextSearchColumns, init);
    }

    @Override
    public void prune(int time) {
        if (blockchainConfig.isEnablePruning()) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection();
                 @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT)
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table + " WHERE transaction_timestamp < ? LIMIT " + propertiesHolder.BATCH_COMMIT_SIZE())) {
                pstmt.setInt(1, time - blockchainConfig.getMaxPrunableLifetime());
                int deleted;
                do {
                    deleted = pstmt.executeUpdate();
                    if (deleted > 0) {
                        LOG.debug("Deleted " + deleted + " expired prunable data from " + table);
                    }
                    dataSource.commit(false);
                } while (deleted >= propertiesHolder.BATCH_COMMIT_SIZE());
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    public MinMaxValue getMinMaxValue(int height, int currentTime) {
        // select MIN and MAX dbId values in one query
        @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
        String selectMinSql = String.format("SELECT IFNULL(min(DB_ID), 0) as min_id, " +
                "IFNULL(max(DB_ID), 0) as max_id, IFNULL(count(*), 0) as count, max(height) as max_height from %s where HEIGHT <= ? and transaction_timestamp >= ?", table);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(selectMinSql)) {
            pstmt.setInt(1, height);
            pstmt.setInt(2, currentTime - blockchainConfig.getMinPrunableLifetime());
            MinMaxValue minMaxValue = this.getMinMaxValue(pstmt);
            minMaxValue.setColumn("db_id");
            return minMaxValue;
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
