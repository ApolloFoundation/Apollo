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

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

@Slf4j
public abstract class PrunableDbTable<T extends DerivedEntity> extends EntityDbTable<T> {

    private final BlockchainConfig blockchainConfig;
    public final PropertiesHolder propertiesHolder;

    public PrunableDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns,
                           DatabaseManager databaseManager,
                           BlockchainConfig blockchainConfig,
                           PropertiesHolder propertiesHolder,
                           Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(table, dbKeyFactory, multiversion, fullTextSearchColumns, databaseManager, deleteOnTrimDataEvent);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
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
                        log.debug("Deleted " + deleted + " expired prunable data from " + table);
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
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
