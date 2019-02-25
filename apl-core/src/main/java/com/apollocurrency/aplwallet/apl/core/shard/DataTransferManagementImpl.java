package com.apollocurrency.aplwallet.apl.core.shard;

import java.io.IOException;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

/**
 * {@inheritDoc}
 */
public class DataTransferManagementImpl implements DataTransferManagement {

    private MigrateState state = MigrateState.INIT;
    private DbProperties dbProperties;
    private DatabaseManager databaseManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState getCurrentState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long moveData(DatabaseMetaInfo source, DatabaseMetaInfo target) throws SQLException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target) throws IOException {
        return false;
    }
}
