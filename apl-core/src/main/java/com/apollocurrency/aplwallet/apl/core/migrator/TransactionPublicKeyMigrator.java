/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.inject.Inject;

public class TransactionPublicKeyMigrator {
    private static final Logger LOG = getLogger(PublicKeyMigrator.class);
    private PropertiesHolder propertiesHolder;
    private DatabaseManager databaseManager;

    @Inject
    public TransactionPublicKeyMigrator(PropertiesHolder propertiesHolder, DatabaseManager databaseManager) {
        this.propertiesHolder = propertiesHolder;
        this.databaseManager = databaseManager;
    }

    public void migrate() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Connection con;
        boolean isInTransaction = false;
        // start transaction or use already started
        try {
            if (dataSource.isInTransaction()) {
                isInTransaction = true;
            } else {
                dataSource.begin();
            }
            con = dataSource.getConnection();

            try (PreparedStatement checkTxStmt = con.prepareStatement("select 1 from transaction where sender_public_key IS NOT NULL");
                 PreparedStatement selectPublicKeyStmt = con.prepareStatement("select * from public_key where latest = true AND public_key is not null");
                 PreparedStatement updateTxStmt = con.prepareStatement("update transaction set sender_public_key = ? where sender_id = ? and height = ? order by db_id")
            ) {
                long updateCount = 0;
                try(ResultSet rs = checkTxStmt.executeQuery()) {
                    if (rs.next()) {
                        updateCount = rs.getLong(1);
                    }
                }
                if (updateCount == 0) {
                    LOG.info("Will assign public keys for transactions");
                    try (ResultSet rs = selectPublicKeyStmt.executeQuery()) {
                        int assigned = 0;
                        while (rs.next()) {
                            int height = rs.getInt("height");
                            long accountId = rs.getLong("account_id");
                            byte[] publicKey = rs.getBytes("public_key");
                            updateTxStmt.setBytes(1, publicKey);
                            updateTxStmt.setLong(2, accountId);
                            updateTxStmt.setInt(3, height);
                            updateTxStmt.executeUpdate();
                            assigned++;
                        }
                        LOG.info("Assigned {} public keys", assigned);
                    }
                } else {
                    LOG.info("{} public keys already assigned", updateCount);
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
