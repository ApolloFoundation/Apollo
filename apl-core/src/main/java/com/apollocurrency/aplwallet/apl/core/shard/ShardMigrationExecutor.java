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

package com.apollocurrency.aplwallet.apl.core.shard;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;

/**
 * Component for starting sharding process which contains several steps/states.
 *
 * @author yuriy.larin
 */
@Singleton
public class ShardMigrationExecutor {

    private final List<DataMigrateOperation> dataMigrateOperations = new ArrayList<>();

    public ShardMigrationExecutor() {
    }

    public MigrateState executeOperation(DataMigrateOperation shardOperation) {
        dataMigrateOperations.add(shardOperation);
        return shardOperation.execute();
    }

}
