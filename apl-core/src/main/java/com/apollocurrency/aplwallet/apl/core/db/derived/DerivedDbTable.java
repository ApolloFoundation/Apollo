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

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.util.StringValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;

public abstract class DerivedDbTable<T> implements DerivedTableInterface<T> {

    private FullTextConfig fullTextConfig;
    private DerivedTablesRegistry derivedDbTablesRegistry;

    protected final String table;
    protected DatabaseManager databaseManager;

    //TODO: fix injects and remove
    private void lookupCdi(){
        if(fullTextConfig==null){
            fullTextConfig =  CDI.current().select(FullTextConfig.class).get();
        }
        if(derivedDbTablesRegistry==null){
            derivedDbTablesRegistry = CDI.current().select(DerivedTablesRegistry.class).get();
        }
    }

    // We should find better place for table init
    protected DerivedDbTable(String table, boolean init) { // for CDI beans setUp 'false'
        StringValidator.requireNonBlank(table, "Table name");
        this.table = table;
        databaseManager = CDI.current().select(DatabaseManager.class).get();
        if (init) {
            init();
        }
    }
    protected DerivedDbTable(String table) {
        this(table, true);
    }

    @Override
    public void trim(int height, TransactionalDataSource dataSource) {}

    @Override
    public void trim(int height) {}

    @PostConstruct
    public void init() {
        lookupCdi();
        derivedDbTablesRegistry.registerDerivedTable(this);
        fullTextConfig.registerTable(table);
    }

    @Override
    public void rollback(int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }



    @Override
    public void truncate() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE " + table);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

/*
    @Override
    @Override
    public void trim(int height, TransactionalDataSource dataSource) {

        //nothing to trim
    }
*/

    public  DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

/*
    @Override
    public void createSearchIndex(Connection con) throws SQLException {
        //implemented in EntityDbTable only
    }
*/

/*
    @Override
    public boolean isPersistent() {
        return false;
    }
*/

    @Override
    public final String toString() {
        return table;
    }

}
