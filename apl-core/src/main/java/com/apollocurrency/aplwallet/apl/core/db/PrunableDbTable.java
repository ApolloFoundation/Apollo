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

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import org.slf4j.Logger;

public abstract class PrunableDbTable<T> extends PersistentDbTable<T> {
    private static final Logger LOG = getLogger(PrunableDbTable.class);
    private final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, fullTextSearchColumns);
    }

    PrunableDbTable(String table, DbKey.Factory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        super(table, dbKeyFactory, multiversion, fullTextSearchColumns);
    }

    protected PrunableDbTable(DbKey.Factory<T> dbKeyFactory, boolean multiversion, String fullTextSearchColumns) {
        super("", dbKeyFactory, multiversion, fullTextSearchColumns);
    }

    @Override
    public final void trim(int height) {
        prune();
        super.trim(height);
    }

    protected void prune() {
        if (blockchainConfig.isEnablePruning()) {
            try (Connection con = db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + table + " WHERE transaction_timestamp < ? LIMIT " + Constants.BATCH_COMMIT_SIZE)) {
                pstmt.setInt(1, AplCore.getEpochTime() - blockchainConfig.getMaxPrunableLifetime());
                int deleted;
                do {
                    deleted = pstmt.executeUpdate();
                    if (deleted > 0) {
                        LOG.debug("Deleted " + deleted + " expired prunable data from " + table);
                    }
                    db.commitTransaction();
                } while (deleted >= Constants.BATCH_COMMIT_SIZE);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

}
