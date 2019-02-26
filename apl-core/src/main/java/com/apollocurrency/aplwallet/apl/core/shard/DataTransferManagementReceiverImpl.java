package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
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

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager) {
        this.dbProperties = databaseManager.getBaseDbProperties();
        this.databaseManager = databaseManager;
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
        try {
            TransactionalDataSource temporaryDb = databaseManager.createAndAddTemporaryDb(source.getNewFileName());
            // add info about state
            databaseManager.shutdown(temporaryDb);
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
}
