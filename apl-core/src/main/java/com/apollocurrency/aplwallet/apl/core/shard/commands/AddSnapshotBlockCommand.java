package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.TEMP_DB_CREATED;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command adds snapshot block into empty temp db.
 */
public class AddSnapshotBlockCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(AddSnapshotBlockCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
//    private Blockchain blockchain;
    private Block snapshotBlock;

    public AddSnapshotBlockCommand(DataTransferManagementReceiver dataTransferManagement, /*Blockchain  blockchain, */Block snapshotBlock) {
        this.dataTransferManagement = dataTransferManagement;
//        this.blockchain = blockchain;
        this.snapshotBlock = snapshotBlock;
    }

    @Override
    public MigrateState execute() {
        log.debug("Add snapshot block execute...");
        DatabaseMetaInfo databaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, -1,
                MigrateState.SNAPSHOT_BLOCK_CREATED, this.snapshotBlock);

        return dataTransferManagement.addSnapshotBlock(databaseMetaInfo);
    }
}
