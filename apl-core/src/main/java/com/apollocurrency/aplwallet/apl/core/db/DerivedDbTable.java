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

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.util.StringValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.enterprise.inject.spi.CDI;

public abstract class DerivedDbTable {

    protected static final TransactionalDb db = Db.getDb();

    protected final String table;
    private static BlockchainProcessor blockchainProcessor;
    // We should find better place for table init
    protected DerivedDbTable(String table) {
        StringValidator.requireNonBlank(table, "Table name");
        this.table = table;
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        blockchainProcessor.registerDerivedTable(this);
        FullTextConfig.getInstance().registerTable(table);
    }

    public void rollback(int height) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void truncate() {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE " + table);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

    public void createSearchIndex(Connection con) throws SQLException {
        //implemented in EntityDbTable only
    }

    public boolean isPersistent() {
        return false;
    }

    @Override
    public final String toString() {
        return table;
    }

    protected void trimTables() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        blockchainProcessor.trimDerivedTables();
    }
}
