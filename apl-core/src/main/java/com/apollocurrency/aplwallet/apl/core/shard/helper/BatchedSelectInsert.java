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

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.sql.Connection;

/**
 * Helper Interface used for selecting records from main db source and inserting them into target/shard db.
 *
 * @author yuriy.larin
 */
public interface BatchedSelectInsert {

    /**
     * Reset internal structures and state
     */
    void reset();

    /**
     * Method makes select operations on source database then it insert data into target database.
     * It also makes 'relinking' records in several tables to snapshot block at specified height.
     *
     * @return quantity of selected and inserted records per every table
     * @throws Exception
     */
    long selectInsertOperation(
            Connection sourceConnect, Connection targetConnect, TableOperationParams operationParams) throws Exception;
}
