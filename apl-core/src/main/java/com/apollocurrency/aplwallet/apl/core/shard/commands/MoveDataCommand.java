/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver.TEMPORARY_MIGRATION_FILE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command for moving block, transaction data from main into shard database.
 *
 * @author yuriy.larin
 */
public class MoveDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(MoveDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;

    private DatabaseMetaInfo source;
    private DatabaseMetaInfo target;
    private Map<String, Long> tableNameCountMap;

    private String newFileName;
//    private List<Statement> statementList;
    private int commitBatchSize;
    private long snapshotBlockHeight;

    public MoveDataCommand(
            DataTransferManagementReceiver dataTransferManagement,
//            DatabaseMetaInfo source, DatabaseMetaInfo target,
            Map<String, Long> tableNameCountMap,
            String newFileName, /*List<Statement> statementList, */int commitBatchSize,
            Long snapshotBlockHeight) {
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
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("MoveDataCommand execute...");

        DatabaseMetaInfo sourceDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME,
                -1, MigrateState.DATA_MOVING_STARTED, null, null);
        DatabaseMetaInfo targetDatabaseMetaInfo = new DatabaseMetaInfoImpl(
                null, TEMPORARY_MIGRATION_FILE_NAME,
                -1, MigrateState.DATA_MOVING_STARTED, null, this.snapshotBlockHeight);

        return dataTransferManagement.moveData(this.tableNameCountMap, sourceDatabaseMetaInfo, targetDatabaseMetaInfo);
    }
}
