/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public class TransactionPublicKeyMigrator {
    public static final String REQUIRE_MIGRATION_PROPERTY_NAME = "requiredTransactionPublicKeyMigration";
    public static final String LAST_DB_ID_PROPERTY_NAME = "publicKeyLastDbId";
    private static final Logger LOG = getLogger(TransactionPublicKeyMigrator.class);
    private DatabaseManager databaseManager;
    private OptionDAO optionDAO;

    @Inject
    public TransactionPublicKeyMigrator(DatabaseManager databaseManager, OptionDAO optionDAO) {
        this.databaseManager = databaseManager;
        this.optionDAO = optionDAO;
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
                 PreparedStatement selectPublicKeyStmt = con.prepareStatement("select * from public_key where latest = true AND public_key is not null and db_id > ? limit 2000");
                 PreparedStatement updateTxStmt = con.prepareStatement("update transaction set sender_public_key = ? where sender_id = ? and height = ? order by db_id")
            ) {
                long updateCount = 0;
                try (ResultSet rs = checkTxStmt.executeQuery()) {
                    if (rs.next()) {
                        updateCount = rs.getLong(1);
                    }
                }
                String requiredMigration = optionDAO.get(REQUIRE_MIGRATION_PROPERTY_NAME);
                if (requiredMigration == null || requiredMigration.equalsIgnoreCase("true")) {

                    boolean hasMore;
                    long dbId = 0;
                    if (requiredMigration == null) {
                        optionDAO.set(LAST_DB_ID_PROPERTY_NAME, "0");
                        optionDAO.set(REQUIRE_MIGRATION_PROPERTY_NAME, "true");
                    } else {
                        dbId = Long.parseLong(optionDAO.get(LAST_DB_ID_PROPERTY_NAME));
                    }
                    LOG.info("Will assign public keys for transactions starting from {}", dbId);
                    int assigned = 0;
                    do {
                        hasMore = false;
                        selectPublicKeyStmt.setLong(1, dbId);
                        try (ResultSet rs = selectPublicKeyStmt.executeQuery()) {
                            while (rs.next()) {
                                int height = rs.getInt("height");
                                long accountId = rs.getLong("account_id");
                                byte[] publicKey = rs.getBytes("public_key");
                                dbId = rs.getLong("db_id");
                                updateTxStmt.setBytes(1, publicKey);
                                updateTxStmt.setLong(2, accountId);
                                updateTxStmt.setInt(3, height);
                                updateTxStmt.executeUpdate();
                                assigned++;
                                if (assigned % 100 == 0) {
                                    optionDAO.set(LAST_DB_ID_PROPERTY_NAME, String.valueOf(dbId));
                                    dataSource.commit(false);
                                }
                                hasMore = true;
                            }
                        }
                    } while (hasMore);
                    LOG.info("Assigned {} public keys", assigned);
                } else {
                    LOG.info("{} public keys already assigned", updateCount);
                }
            }
            optionDAO.set(REQUIRE_MIGRATION_PROPERTY_NAME, "false");
            dataSource.commit(!isInTransaction);
        } catch (SQLException e) {
            dataSource.rollback(!isInTransaction);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
