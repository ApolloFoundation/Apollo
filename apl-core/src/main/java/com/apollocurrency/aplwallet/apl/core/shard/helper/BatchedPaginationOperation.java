/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.sql.Connection;

import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;

/**
 * Helper Interface used for paginated selecting/deleteing records from main db source and streaming/inserting data into target/shard db.
 *
 * @author yuriy.larin
 */
public interface BatchedPaginationOperation {

    /**
     * Reset internal structures and state
     */
    void reset();

    /**
     * Method makes select operations on source database then it insert data into target database.
     * It also makes 'relinking' records in several tables to snapshot block at specified height.
     *
     * @param sourceConnect low level connection for source
     * @param targetConnect low level connection for target
     * @param operationParams wrapper for several parameters
     * @return quantity of processed records per every table passed inside 'operationParams'
     * @throws Exception
     */
    long processOperation(
            Connection sourceConnect, Connection targetConnect, TableOperationParams operationParams) throws Exception;

    /**
     * We want to assign DAO for keep track on sharding progress.
     * @param dao dao implementation to store/retrieve progress state
     */
    void setShardRecoveryDao(ShardRecoveryDaoJdbc dao);

}
