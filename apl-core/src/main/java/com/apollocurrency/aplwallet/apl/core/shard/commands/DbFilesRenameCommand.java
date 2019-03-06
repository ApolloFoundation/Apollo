package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_DB_CREATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Statement;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.DatabaseMetaInfoImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command for renaming database files.
 */
public class DbFilesRenameCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(DbFilesRenameCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;

    private DatabaseMetaInfo source;
    private DatabaseMetaInfo target;

    private String newFileName;
    private List<Statement> statementList;
    private int commitBatchSize;

    public DbFilesRenameCommand(
            DataTransferManagementReceiver dataTransferManagement,
//            DatabaseMetaInfo source, DatabaseMetaInfo target,
             String newFileName, List<Statement> statementList, int commitBatchSize) {
        this.dataTransferManagement = dataTransferManagement;
//        this.source = source;
//        this.target = target;
        this.newFileName = newFileName;
        this.statementList = statementList;
        this.commitBatchSize = commitBatchSize;
    }

    @Override
    public MigrateState execute() {
        log.debug("DbFilesRenameCommand execute...");

        DatabaseMetaInfo sourceDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME,
                -1, SHARD_DB_CREATED, null);
        DatabaseMetaInfo targetDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME,
                -1, SHARD_DB_CREATED, null);

        return dataTransferManagement.renameDataFiles(sourceDatabaseMetaInfo, targetDatabaseMetaInfo);
    }
}
