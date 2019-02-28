package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager) {
        this.dbProperties = databaseManager.getBaseDbProperties();
        this.databaseManager = databaseManager;
    }

/*
    @Override
    public Map<String, Long> getTableNameWithCountMap() {
        return tableNameCountMap;
    }
*/

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
    public MigrateState moveData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        log.debug("Starting shard data transfer from [{}] tables...", tableNameCountMap.size());
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
        if (lastTableName != null && !lastTableName.isEmpty()) {
            // NOT FINISHED YET!!!
            // check/compare records count in source and target, clear target if needed, reinsert again in case...
        } else {
            // insert data as not processed previously
            long startAllTables = System.currentTimeMillis();
            Connection targetConnect = null;

            String currentTable = null;
            try (
                    Connection sourceConnect = source.getDataSource().getConnection();
            ) {
                for (String tableName : tableNameCountMap.keySet()) {
                    long start = System.currentTimeMillis();
                    if (!targetDataSource.isInTransaction()) {
                        targetConnect = targetDataSource.begin();
                    }
                    currentTable = tableName;
                    optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName, targetDataSource);
                    int totalCount = generateInsertStatements(sourceConnect, targetConnect, tableName, target.getCommitBatchSize());
                    targetDataSource.commit(false);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                }
            } catch (Exception e) {
                log.error("Error processing table = '" + currentTable + "'", e);
                targetDataSource.rollback();
            } finally {
                if (targetConnect != null) {
                    targetDataSource.commit();
                }
            }
            log.debug("Processed table(s)=[{}] in {} secs", tableNameCountMap.size(), (System.currentTimeMillis() - startAllTables)/1000);
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

        Statement selectStmt = sourceConnect.createStatement();
        long lowerIndex = 0, upperIndex = batchCommitSize;

        int insertedBeforeBatchCount = 0;
        int totalInsertedCount = 0;
        String unFormattedSql;
        if (tableName.equalsIgnoreCase("block")) {
            unFormattedSql = "SELECT * FROM %s where HEIGHT BETWEEN %d AND %d limit %d";
        } else {
//            unFormattedSql = "SELECT * FROM %s where HEIGHT BETWEEN %d AND %d limit %d";
            unFormattedSql = "SELECT * FROM %s";
        }
        String sqlToExecute;

        long startSelect = System.currentTimeMillis();
        ResultSet rs = null;
        if (tableName.equalsIgnoreCase("block")) {
            sqlToExecute = String.format(unFormattedSql, tableName, lowerIndex, upperIndex, batchCommitSize);
            rs = selectStmt.executeQuery(sqlToExecute);
        } else {
            rs = selectStmt.executeQuery("SELECT * FROM " + tableName);
        }
        log.debug("Select '{}' in {} secs", tableName, (System.currentTimeMillis() - startSelect)/1000);
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        int[] columnTypes = new int[numColumns];

        StringBuilder columnNames = new StringBuilder();
        StringBuilder columnQuestionMarks = new StringBuilder();
        for (int i = 0; i < numColumns; i++) {
            columnTypes[i] = rsmd.getColumnType(i + 1);
            if (i != 0) {
                columnNames.append(",");
                columnQuestionMarks.append("?,");
            }
            columnNames.append(rsmd.getColumnName(i + 1));
        }
        columnQuestionMarks.append("?");

        // no need to synch by StringBuffer implementation
        StringBuilder sqlInsertString = new StringBuilder(8000);
        sqlInsertString.append("INSERT INTO ").append(tableName)
                .append("(").append(columnNames).append(")").append(" values (").append(columnQuestionMarks).append(")");

        if (log.isTraceEnabled()) {
            log.trace(sqlInsertString.toString());
        }
        try (
                // precompile sql
                PreparedStatement preparedStatement = targetConnect.prepareStatement(sqlInsertString.toString())
        ) {
            long startInsert = System.currentTimeMillis();
            while (rs.next()) {
                for (int i = 0; i < numColumns; i++) {
                    // bind values
                    preparedStatement.setObject(i + 1, rs.getObject(i + 1));
                }
                insertedBeforeBatchCount += preparedStatement.executeUpdate();
                if (insertedBeforeBatchCount >= batchCommitSize) {
                    targetConnect.commit();
                    totalInsertedCount += insertedBeforeBatchCount;
                    insertedBeforeBatchCount = 0;
                    log.debug("Partial commit = {}", totalInsertedCount);
                }
            }
            targetConnect.commit(); // commit latest records if any
            totalInsertedCount += insertedBeforeBatchCount;
            log.debug("Finished '{}' inserted [{}] in = {} sec", tableName, totalInsertedCount, (System.currentTimeMillis() - startInsert)/1000);
        } catch (Exception e) {
            int errorTotalCount = totalInsertedCount + insertedBeforeBatchCount;
            log.error("Insert error on record count=[" + errorTotalCount + "] by SQL =\n" + sqlInsertString.toString() + "\n", e);
        }
        return totalInsertedCount;
    }
}
