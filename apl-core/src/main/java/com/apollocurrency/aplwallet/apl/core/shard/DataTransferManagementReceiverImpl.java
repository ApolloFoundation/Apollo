package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class DataTransferManagementReceiverImpl implements DataTransferManagementReceiver {
    private static final Logger log = getLogger(DataTransferManagementReceiverImpl.class);

    private MigrateState state = MigrateState.INIT;
    private DbProperties dbProperties;
    private DatabaseManager databaseManager;
    private OptionDAO optionDAO;
    private Map<String, Long> tableNameCountMap = new LinkedHashMap<>(10);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager) {
        this.dbProperties = databaseManager.getBaseDbProperties();
        this.databaseManager = databaseManager;
        tableNameCountMap.put("ACCOUNT_LEDGER", -1L);
        tableNameCountMap.put("ACCOUNT", -1L);
    }

    @Override
    public Map<String, Long> getTableNameWithCountMap() {
        return tableNameCountMap;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState getCurrentState() {
        return state;
    }

    @Override
    public MigrateState createTempDb(DatabaseMetaInfo source) {
        log.debug("Creating TEMP db file...");
        Objects.requireNonNull(source, "source meta-info is NULL");
        try {
            TransactionalDataSource temporaryDb = databaseManager.createAndAddTemporaryDb(source.getNewFileName());
            // add info about state
//            databaseManager.shutdown(temporaryDb);
            return MigrateState.TEMP_DB_CREATED;
        } catch (Exception e) {
            log.error("Error creation Temp Db", e);
            return MigrateState.FAILED;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState moveData(DatabaseMetaInfo source, DatabaseMetaInfo target) {
        log.debug("Starting shard data transfer...");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        TransactionalDataSource mainDs;
        TransactionalDataSource tempDs;
        String lastTableName = null;
        if (source.getDataSource() == null) {
            mainDs = databaseManager.getDataSource();
            source.setDataSource(mainDs);
        }
        TransactionalDataSource targetDataSource = target.getDataSource();
        if (targetDataSource == null) {
            tempDs = databaseManager.getShardDataSourceById(-1L);
            target.setDataSource(tempDs);
            targetDataSource = tempDs;
        }
        this.optionDAO = new OptionDAO(this.databaseManager); // actually we want to use TEMP=TARGET data source in OptionDAO
        if (optionDAO.get(PREVIOUS_MIGRATION_KEY, targetDataSource) == null) {
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING.name(), targetDataSource);
        } else {
            // continue previous run
            lastTableName = optionDAO.get(LAST_MIGRATION_OBJECT_NAME, targetDataSource);
        }
        if (lastTableName == null || lastTableName.isEmpty()) {
            // check/compare records count in source and target, clear target if needed, reinsert again in case...
        } else {
            // insert data as not processed previously
            for (String tableName : tableNameCountMap.keySet()) {
                try (
                        Connection sourceConnect = source.getDataSource().getConnection();
                        Connection targetConnect = targetDataSource.begin();
                ) {
                    optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName, targetDataSource);
                    int totalCount = generateInsertStatements(sourceConnect, targetConnect, tableName, target.getCommitBatchSize());
                    targetDataSource.commit();
                    log.debug("Totally inserted = {} in table ='{}'", totalCount, tableName);
                } catch (Exception e) {
                    log.error("Error processing table = '{}'", e, tableName);
                }
            }
        }
        return MigrateState.DATA_MOVING;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target) {
        log.debug("Starting shard files renaming...");
        return MigrateState.COMPLETED;
    }

    private int generateInsertStatements(Connection sourceConnect, Connection targetConnect, String tableName, long batchCommitSize)
            throws Exception {
        log.debug("Generating Insert statements for: '{}'", tableName);

        Statement stmt = sourceConnect.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        int[] columnTypes = new int[numColumns];
        String columnNames = "";
        for (int i = 0; i < numColumns; i++) {
            columnTypes[i] = rsmd.getColumnType(i + 1);
            if (i != 0) {
                columnNames += ",";
            }
            columnNames += rsmd.getColumnName(i + 1);
        }

        String sqlInsertString = null;
        java.util.Date d = null;
        int insertedCount = 0;
        int totalCount = 0;
        while (rs.next()) {
            String columnValues = "";
            for (int i = 0; i < numColumns; i++) {
                if (i != 0) {
                    columnValues += ",";
                }

                switch (columnTypes[i]) {
                    case Types.BIGINT:
                    case Types.BIT:
                    case Types.BOOLEAN:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.INTEGER:
                    case Types.SMALLINT:
                    case Types.TINYINT:
                        String v = rs.getString(i + 1);
                        columnValues += v;
                        break;

                    case Types.DATE:
                        d = rs.getDate(i + 1);
                    case Types.TIME:
                        if (d == null) d = rs.getTime(i + 1);
                    case Types.TIMESTAMP:
                        if (d == null) d = rs.getTimestamp(i + 1);

                        if (d == null) {
                            columnValues += "null";
                        } else {
                            columnValues += "TO_DATE('"
                                    + dateFormat.format(d)
                                    + "', 'YYYY/MM/DD HH24:MI:SS')";
                        }
                        break;

                    default:
                        v = rs.getString(i + 1);
                        if (v != null) {
                            columnValues += "'" + v.replaceAll("'", "''") + "'";
                        } else {
                            columnValues += "null";
                        }
                        break;
                }
            }
            sqlInsertString = String.format("INSERT INTO %s (%s) values (%s)",
                    tableName,
                    columnNames,
                    columnValues);
            log.trace(sqlInsertString);
            try(
                    PreparedStatement preparedStatement = targetConnect.prepareStatement(sqlInsertString);
            ) {
                insertedCount +=  preparedStatement.executeUpdate();
                totalCount += insertedCount;
                if (insertedCount >= batchCommitSize) {
                    targetConnect.commit();
                    insertedCount = 0;
                    log.debug("Totally inserted = {}", totalCount);
                }
            } catch (Exception e) {
                log.error("Insert error on SQL = " + sqlInsertString + "\n", e);
            }
        }
        return totalCount;
    }
}
