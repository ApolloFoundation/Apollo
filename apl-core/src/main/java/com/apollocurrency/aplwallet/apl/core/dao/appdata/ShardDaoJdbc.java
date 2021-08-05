package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.converter.db.ShardRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.IntArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.LongArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Shard management + retrieving interface
 */
public interface ShardDao {

    Shard getShardById(@Bind("shardId") long shardId);

    List<Shard> getAllShard();

    long countShard();

    long getNextShardId();

    long getMaxShardId();

    void saveShard(@BindBean Shard shard);

    int updateShard(@BindBean Shard shard);

    int hardDeleteShard(@Bind("shardId") long shardId);

    int hardDeleteAllShards();

    Shard getShardAtHeight(@Bind("height") long height);

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
