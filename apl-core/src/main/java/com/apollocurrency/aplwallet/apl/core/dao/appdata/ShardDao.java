package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.converter.db.ShardRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.IntArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.LongArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
/*import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
*/
import java.util.List;

/**
 * Shard management + retrieving interface
 */
public interface ShardDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard where shard_id = :shardId")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getShardById(@Bind("shardId") long shardId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard")
    @RegisterRowMapper(ShardRowMapper.class)
    List<Shard> getAllShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM shard")
    long countShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT IFNULL(max(SHARD_ID), 0) + 1 as shard_id FROM shard")
    @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
    long getNextShardId();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT IFNULL(max(SHARD_ID), 0) FROM shard")
    @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
    long getMaxShardId();

    @Transactional
    @SqlUpdate("INSERT INTO shard(shard_id, shard_hash, shard_state, shard_height, zip_hash_crc, prunable_zip_hash, generator_ids, block_timeouts, block_timestamps) " +
        "VALUES (:shardId, :shardHash, :shardState, :shardHeight, :coreZipHash, :prunableZipHash, :generatorIds, " +
        ":blockTimeouts, :blockTimestamps)")
    @RegisterArgumentFactory(LongArrayArgumentFactory.class)
    @RegisterArgumentFactory(IntArrayArgumentFactory.class)
    void saveShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("UPDATE shard SET shard_hash =:shardHash, shard_state =:shardState, shard_height =:shardHeight, " +
        "zip_hash_crc =:coreZipHash, prunable_zip_hash =:prunableZipHash, generator_ids =:generatorIds, block_timeouts =:blockTimeouts, block_timestamps =:blockTimestamps " +
        "where shard_id =:shardId")
    @RegisterArgumentFactory(LongArrayArgumentFactory.class)
    @RegisterArgumentFactory(IntArrayArgumentFactory.class)
    int updateShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("DELETE FROM shard where shard_id =:shardId")
    int hardDeleteShard(@Bind("shardId") long shardId);

    @Transactional
    @SqlUpdate("DELETE FROM shard")
    int hardDeleteAllShards();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_height =:height")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getShardAtHeight(@Bind("height") long height);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_height = (SELECT MAX(shard_height) FROM shard)")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getLastShard();

    @Transactional(readOnly = true)
    @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
    @SqlQuery("SELECT IFNULL(max(shard_height), 0) FROM shard")
    int getLatestShardHeight();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_state = 100 ORDER BY shard_height DESC LIMIT 1")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getLastCompletedShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_state = 100 OR shard_state = 50 ORDER BY shard_height DESC LIMIT 1")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getLastCompletedOrArchivedShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_state = 100 ORDER BY shard_height DESC")
    @RegisterRowMapper(ShardRowMapper.class)
    List<Shard> getAllCompletedShards();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_state = 100 OR shard_state = 50 ORDER BY shard_height DESC")
    @RegisterRowMapper(ShardRowMapper.class)
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
    @Transactional(readOnly = true)
//    @SqlQuery("(select * from SHARD where SHARD_STATE = 100 and SHARD_HEIGHT <= :heightFrom + 1 limit 1) UNION ALL" +
//        " (select * from SHARD where SHARD_STATE = 100 and (SHARD_HEIGHT > :heightFrom + 1 OR SHARD_HEIGHT >= :heightTo) order by SHARD_HEIGHT)")
    @SqlQuery("select * from shard WHERE SHARD_STATE = 100 and (SHARD_HEIGHT between :heightFrom - 1 and :heightTo) order by SHARD_HEIGHT")
    @RegisterRowMapper(ShardRowMapper.class)
    List<Shard> getCompletedBetweenBlockHeight(@Bind("heightFrom") long heightFrom, @Bind("heightTo") long heightTo);
}
