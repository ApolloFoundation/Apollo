/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class UnconfirmedTransactionTable extends EntityDbTable<UnconfirmedTransactionEntity> {

    private static final LongKeyFactory<UnconfirmedTransactionEntity> transactionKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(UnconfirmedTransactionEntity unconfirmedTransaction) {
            return new LongKey(unconfirmedTransaction.getId());
        }
    };

    private final UnconfirmedTransactionEntityRowMapper entityRowMapper;

    private final IteratorToStreamConverter<UnconfirmedTransactionEntity> streamConverter;

    @Inject
    public UnconfirmedTransactionTable(UnconfirmedTransactionEntityRowMapper entityRowMapper,
                                       DatabaseManager databaseManager,
                                       Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("unconfirmed_transaction", transactionKeyFactory, false, null,
                databaseManager, deleteOnTrimDataEvent);
        this.entityRowMapper = entityRowMapper;
        this.streamConverter = new IteratorToStreamConverter<>();
    }

    @Override
    public UnconfirmedTransactionEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        UnconfirmedTransactionEntity entity = entityRowMapper.map(rs, null);
        return entity;
    }

    @Override
    public void save(Connection con, UnconfirmedTransactionEntity unconfirmedTransactionEntity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO unconfirmed_transaction (id, transaction_height, "
                + "fee_per_byte, expiration, transaction_bytes, prunable_json, arrival_timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, unconfirmedTransactionEntity.getId());
            pstmt.setInt(++i, unconfirmedTransactionEntity.getHeight());
            pstmt.setLong(++i, unconfirmedTransactionEntity.getFeePerByte());
            pstmt.setInt(++i, unconfirmedTransactionEntity.getExpiration());
            pstmt.setBytes(++i, unconfirmedTransactionEntity.getTransactionBytes());
            String prunableJSONString = unconfirmedTransactionEntity.getPrunableAttachmentJsonString();
            if (prunableJSONString != null) {
                pstmt.setString(++i, prunableJSONString);
            } else {
                pstmt.setNull(++i, Types.VARCHAR);
            }
            pstmt.setLong(++i, unconfirmedTransactionEntity.getArrivalTimestamp());
            pstmt.setInt(++i, unconfirmedTransactionEntity.getHeight());
            pstmt.executeUpdate();
        }
    }

    public int deleteById(long id) {
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM unconfirmed_transaction WHERE id = ?")) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            databaseManager.getDataSource().rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }

    public UnconfirmedTransactionEntity getById(long id) {
        return get(getTransactionKeyFactory().newKey(id));
    }

    @Override
    public String defaultSort() {
        return " ORDER BY transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC, id ASC ";
    }

    public Stream<UnconfirmedTransactionEntity> getExpiredTxsStream(int time) {
        return streamConverter.apply(getManyBy(new DbClause.IntClause("expiration", DbClause.Op.LT, time), 0, -1, ""));
    }

    public int countExpiredTransactions(int epochTime) {
        return super.getCount(
            new DbClause.IntClause("expiration", DbClause.Op.LT, epochTime));
    }

    public Stream<UnconfirmedTransactionEntity> getAllUnconfirmedTransactions() {
        return streamConverter.apply(this.getAll(0, -1));
    }

    @Override
    public int rollback(int height) {
        return 0;
    }

    public List<Long> getAllUnconfirmedTransactionIds() {
        List<Long> result = new ArrayList<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM unconfirmed_transaction");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    public LongKeyFactory<UnconfirmedTransactionEntity> getTransactionKeyFactory() {
        return transactionKeyFactory;
    }

}
