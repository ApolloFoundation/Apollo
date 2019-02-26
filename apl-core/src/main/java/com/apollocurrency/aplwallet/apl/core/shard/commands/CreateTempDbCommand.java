package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.TEMP_DB_CREATED;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

public class CreateTempDbCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CreateTempDbCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;

    public CreateTempDbCommand(DataTransferManagementReceiver dataTransferManagement) {
        this.dataTransferManagement = dataTransferManagement;
    }

    @Override
    public MigrateState execute() {
        log.debug("CreateTempDbCommand execute...");
//        if (dataTransferManagement.getCurrentState() == MigrateState.INIT) {
//            DbProperties dbProperties = dataTransferManagement.getDatabaseManager().getBaseDbProperties();
//        }
        DatabaseMetaInfo databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, -1, TEMP_DB_CREATED);

        return dataTransferManagement.createTempDb(databaseMetaInfo);
    }
}
