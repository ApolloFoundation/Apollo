/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.converter.db.ShardRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.IntArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.LongArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;

import java.util.List;
import java.sql.Connection;

/**
 * Shard management + retrieving class
 */
public class ShardDaoJdbcImpl extends ShardDaoJdbc {

    Shard getShardById(Connection con,  long shardId)
    {
	return new Shard();
    };

    List<Shard> getAllShard(Connection con)
    {
	return new List(Shard);
    };

    long countShard(Connection con)
    {
	return 0;
    };

    long getNextShardId(Connection con)
    {
	return 0;
    };

    long getMaxShardId(Connection com)
    {
	return 0;
    };

    void saveShard(@BindBean Shard shard)
    {
	return 0;
    };

    int updateShard(@BindBean Shard shard)
    {
	return 0;
    };

    int hardDeleteShard(@Bind("shardId") long shardId)
    {
	return 0;
    };

    int hardDeleteAllShards()
    {
	return 0;
    };

    Shard getShardAtHeight(@Bind("height") long height)
    {
	return new Shard();
    };

    Shard getLastShard();

    int getLatestShardHeight();

    Shard getLastCompletedShard();

    Shard getLastCompletedOrArchivedShard();

    List<Shard> getAllCompletedShards();

    List<Shard> getAllCompletedOrArchivedShards();

    /**
     * Should select NONE, ONE or SEVERAL shard records by specified lower and upper height limits.
     * Records should go one after another with increasing height and SHARD_STATE = 50 (shard from archive) / 100 (full shard).
     * The method has some limitations because of assumption for consecutive shard records and correct SHARD_STATE.
     * If records are not consecutive with increasing height and with STATE NOT = 100 when it gives incorrect result.
     * We can't have NONE consecutive records in a database usually.
     *
     * @param heightFrom lower block's limit to search for correct shard it stored in
     * @param heightTo upper block's limit to search for correct shard it stored in
     * @return NONE, ONE or TWO consecutive shard records.
     */
    List<Shard> getCompletedBetweenBlockHeight(@Bind("heightFrom") long heightFrom, @Bind("heightTo") long heightTo);
}
