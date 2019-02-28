package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.TEMP_DB_CREATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

public class MoveDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(MoveDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;

    private DatabaseMetaInfo source;
    private DatabaseMetaInfo target;
    private Map<String, Long> tableNameCountMap;

    private String newFileName;
    private List<Statement> statementList;
    private int commitBatchSize;

    public MoveDataCommand(
            DataTransferManagementReceiver dataTransferManagement,
//            DatabaseMetaInfo source, DatabaseMetaInfo target,
            Map<String, Long> tableNameCountMap,
            String newFileName, List<Statement> statementList, int commitBatchSize) {
        this.dataTransferManagement = dataTransferManagement;
        this.tableNameCountMap = tableNameCountMap;
//        this.source = source;
//        this.target = target;
        this.newFileName = newFileName;
        this.statementList = statementList;
        this.commitBatchSize = commitBatchSize;
    }

    @Override
    public MigrateState execute() {
        log.debug("MoveDataCommand execute...");

        DatabaseMetaInfo sourceDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, -1, MigrateState.DATA_MOVING);
        DatabaseMetaInfo targetDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME, null, -1, MigrateState.DATA_MOVING);

        return dataTransferManagement.moveData(this.tableNameCountMap, sourceDatabaseMetaInfo, targetDatabaseMetaInfo);
    }
}
