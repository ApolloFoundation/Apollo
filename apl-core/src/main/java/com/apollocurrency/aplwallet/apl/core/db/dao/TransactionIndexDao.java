package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.TransactionIndexRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
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
     * @param blockId hsard id
     * @param limit limit number or rows
     * @return found records list
     */
    @Transactional(readOnly = true)
    @SqlQuery("SELECT transaction_id, block_id FROM transaction_shard_index WHERE block_id =:blockId LIMIT :limit")
    @RegisterBeanMapper(TransactionIndex.class)
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    List<TransactionIndex> getByBlockId(@Bind("blockId") long blockId, @Bind("limit") long limit);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM transaction_shard_index where transaction_id =:transactionId")
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    TransactionIndex getByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT b.shard_id, b.block_id FROM transaction_shard_index join block_index b on transaction_shard_index.block_id = b.block_id where transaction_id =:transactionId")
    Long getShardIdByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM transaction_shard_index")
    @RegisterRowMapper(TransactionIndexRowMapper.class)
    List<TransactionIndex> getAllTransactionIndex();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM transaction_shard_index where block_id =:blockId")
    long countTransactionIndexByBlockId(@Bind("blockId") long blockId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT block_height FROM block_index LEFT JOIN transaction_shard_index on block_index.block_id = transaction_shard_index.block_id where transaction_id = :transactionId")
    Integer getTransactionHeightByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional
    @SqlUpdate("INSERT INTO transaction_shard_index(transaction_id, block_id) VALUES (:transactionId, :blockId)")
    void saveTransactionIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("UPDATE transaction_shard_index SET block_id =:blockId where transaction_id =:transactionId")
    int updateBlockIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("DELETE FROM transaction_shard_index where transaction_id =:transactionId AND block_id =:blockId")
    int hardDeleteTransactionIndex(@BindBean TransactionIndex transactionIndex);

    @Transactional
    @SqlUpdate("DELETE FROM transaction_shard_index")
    int hardDeleteAllTransactionIndex();

}
