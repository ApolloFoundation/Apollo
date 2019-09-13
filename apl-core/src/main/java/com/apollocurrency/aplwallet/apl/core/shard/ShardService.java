/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import lombok.Setter;

import java.util.List;
import javax.inject.Inject;

public class ShardService {
    @Inject
    @Setter
    private ShardDao shardDao;

    public List<Shard> getAllCompletedShards() {
        return shardDao.getAllCompletedShards();
    }

    public List<Shard> getAllCompletedOrArchivedShards() {
        return shardDao.getAllCompletedOrArchivedShards();
    }

    public List<Shard> getAllShards() {
        return shardDao.getAllShard();
    }
}
