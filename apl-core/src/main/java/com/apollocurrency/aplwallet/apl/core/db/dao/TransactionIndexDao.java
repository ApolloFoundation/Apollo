/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionIndexRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Global Transaction Index management + retrieving interface
 */
public interface TransactionIndexDao {

    /**
     * For Unit tests ONLY
     *
     * @param height height of block
     * @param limit  limit number or rows
     * @return found records list
     */
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM transaction_shard_index WHERE height =:height ORDER BY transaction_index FETCH FIRST :limit ROWS ONLY")
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    List<TransactionIndex> getByBlockHeight(@Bind("height") int height, @Bind("limit") long limit);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM transaction_shard_index where transaction_id =:transactionId")
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    TransactionIndex getByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(transaction_id) FROM transaction_shard_index where transaction_id =:transactionId")
    long countByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT shard_id FROM shard where shard_height > (select height from transaction_shard_index where transaction_id =:transactionId) ORDER BY shard_height LIMIT 1")
    Long getShardIdByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM transaction_shard_index order by height, transaction_index")
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    List<TransactionIndex> getAllTransactionIndex();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM transaction_shard_index where height =:height")
    long countTransactionIndexByBlockHeight(@Bind("height") int height);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM transaction_shard_index where height < IFNULL((select shard_height from shard where shard_id =:shardId),0) AND height >= IFNULL((select shard_height from shard where shard_height < (select shard_height from shard where shard_id =:shardId) ORDER BY height desc LIMIT 1),0)")
    @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
    long countTransactionIndexByShardId(@Bind("shardId") long shardId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT height from transaction_shard_index where transaction_id = :transactionId")
    Integer getTransactionHeightByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional
    @SqlUpdate("INSERT INTO transaction_shard_index(transaction_id, partial_transaction_hash, height, transaction_index) VALUES (:transactionId, :partialTransactionHash, :height, :transactionIndex)")
    void saveTransactionIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("UPDATE transaction_shard_index SET height =:height, transaction_index=:transactionIndex where transaction_id =:transactionId")
    int updateBlockIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("DELETE FROM transaction_shard_index where transaction_id =:transactionId")
    int hardDeleteTransactionIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("DELETE FROM transaction_shard_index")
    int hardDeleteAllTransactionIndex();

}
