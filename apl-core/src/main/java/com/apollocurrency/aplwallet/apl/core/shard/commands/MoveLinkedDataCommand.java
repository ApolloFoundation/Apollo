package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

public class MoveLinkedDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(MoveLinkedDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;

    private DatabaseMetaInfo source;
    private DatabaseMetaInfo target;
    private Map<String, Long> tableNameCountMap;

    private String newFileName;
//    private List<Statement> statementList;
    private int commitBatchSize;

    public MoveLinkedDataCommand(
            DataTransferManagementReceiver dataTransferManagement,
            Map<String, Long> tableNameCountMap,
            String newFileName, /*List<Statement> statementList, */int commitBatchSize) {
        this.dataTransferManagement = dataTransferManagement;
        if (dataTransferManagement != null) {
            this.tableNameCountMap = tableNameCountMap;
        } else {
            // default list
        }
        if (newFileName != null) {
            this.newFileName = newFileName;
        } else {
            this.newFileName = TEMPORARY_MIGRATION_FILE_NAME;
        }
//        this.statementList = statementList;
        this.commitBatchSize = commitBatchSize;
    }

    @Override
    public MigrateState execute() {
        log.debug("MoveDataCommand execute...");

        DatabaseMetaInfo sourceDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                dataTransferManagement.getDatabaseManager().getDataSource(), this.newFileName,
                -1, MigrateState.DATA_MOVING_STARTED, null);
        DatabaseMetaInfo targetDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                dataTransferManagement.getDatabaseManager().getOrCreateShardDataSourceById(-1L), this.newFileName,
                -1, MigrateState.DATA_MOVING_STARTED, null);

        return dataTransferManagement.moveDataBlockLinkedData(this.tableNameCountMap, sourceDatabaseMetaInfo, targetDatabaseMetaInfo);
    }
}
