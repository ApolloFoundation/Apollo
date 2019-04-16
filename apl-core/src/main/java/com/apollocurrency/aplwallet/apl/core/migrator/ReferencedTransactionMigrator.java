/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReferencedTransactionMigrator {
    private static final Logger LOG = getLogger(PublicKeyMigrator.class);
    private DatabaseManager databaseManager;

    @Inject
    public ReferencedTransactionMigrator(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void migrate() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        boolean isInTransaction = false;
        // start transaction or use already started
        try {
            if (dataSource.isInTransaction()) {
                isInTransaction = true;
            } else {
                dataSource.begin();
            }
            Connection con = dataSource.getConnection();

            try (
                    PreparedStatement selectPstmt = con.prepareStatement((
                            "SELECT rtx.transaction_id as id, tx.height FROM referenced_transaction rtx left join transaction  tx  on rtx.transaction_id = tx.id where rtx.height = -1"));
                    PreparedStatement updatePstmt = con.prepareStatement("UPDATE referenced_transaction SET height = ? WHERE transaction_id = ?")
            ) {
                int counter = 0;
                try (ResultSet rs = selectPstmt.executeQuery()) {
                    while (rs.next()) {
                        updatePstmt.setInt(1, rs.getInt("height"));
                        updatePstmt.setLong(2, rs.getLong("id"));
                        updatePstmt.executeUpdate();
                        if (++counter % 1000 == 0) {
                            LOG.info("Migrated {} referenced transactions");
                        }
                    }
                }
                if (counter == 0) {
                    LOG.info("No referenced transactions for migration");
                } else {
                    LOG.info("Total migrated: {} referenced transactions", counter);
                }
            }
            dataSource.commit(!isInTransaction);
        }
        catch (SQLException e) {
            dataSource.rollback(!isInTransaction);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
