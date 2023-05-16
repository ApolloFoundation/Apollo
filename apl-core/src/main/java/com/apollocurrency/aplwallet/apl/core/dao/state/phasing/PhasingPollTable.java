/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.phasing.PhasingPollMapper;
import com.apollocurrency.aplwallet.apl.core.dao.JdbcQueryExecutionHelper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class PhasingPollTable extends EntityDbTable<PhasingPoll> {
    static final LongKeyFactory<PhasingPoll> KEY_FACTORY = new LongKeyFactory<PhasingPoll>("id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            if (poll.getDbKey() == null) {
                poll.setDbKey(KEY_FACTORY.newKey(poll.getId()));
            }
            return poll.getDbKey();
        }
    };

    private final PhasingPollMapper MAPPER = new PhasingPollMapper(KEY_FACTORY);
    private final JdbcQueryExecutionHelper<TransactionEntity> txQueryExecutionHelper;

    @Inject
    public PhasingPollTable(DatabaseManager databaseManager,
                            TransactionEntityRowMapper transactionRowMapper,
                            Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("phasing_poll", KEY_FACTORY, false, null,
                databaseManager, fullTextOperationDataEvent);
        this.txQueryExecutionHelper = new JdbcQueryExecutionHelper<>(databaseManager.getDataSource(), (rs) -> transactionRowMapper.map(rs, null));
    }


    @Override
    public PhasingPoll load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        PhasingPoll poll = MAPPER.map(rs, null);
        return poll;
    }

    public PhasingPoll get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    public void save(Connection con, PhasingPoll poll) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll (id, account_id, "
            + "finish_height,  finish_time, whitelist_size, voting_model, quorum, min_balance, holding_id, "
            + "min_balance_model, hashed_secret, algorithm, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, poll.getId());
            pstmt.setLong(++i, poll.getAccountId());
            pstmt.setInt(++i, poll.getFinishHeight());
            pstmt.setInt(++i, poll.getFinishTime());
            pstmt.setByte(++i, (byte) poll.getWhitelist().length);
            VoteWeighting voteWeighting = poll.getVoteWeighting();
            pstmt.setByte(++i, voteWeighting.getVotingModel().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, poll.getQuorum());
            DbUtils.setLongZeroToNull(pstmt, ++i, voteWeighting.getMinBalance());
            DbUtils.setLongZeroToNull(pstmt, ++i, voteWeighting.getHoldingId());
            pstmt.setByte(++i, voteWeighting.getMinBalanceModel().getCode());
            DbUtils.setBytes(pstmt, ++i, poll.getHashedSecret());
            pstmt.setByte(++i, poll.getAlgorithm());
            pstmt.setInt(++i, poll.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<TransactionEntity> getFinishingTransactions(int height) {
        return txQueryExecutionHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, phasing_poll " +
                "WHERE phasing_poll.id = transaction.id AND phasing_poll.finish_height = ? " +
                "ORDER BY transaction.height, transaction.transaction_index");
            pstmt.setInt(1, height);
            return pstmt;
        });
    }

    public List<TransactionEntity> getFinishingTransactionsByTime(int startTime, int finishTime) {
        return txQueryExecutionHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, phasing_poll " +
                "WHERE phasing_poll.id = transaction.id AND phasing_poll.finish_height = -1 AND phasing_poll.finish_time > ? AND phasing_poll.finish_time <= ? " +
                "ORDER BY transaction.height, transaction.transaction_index");
            pstmt.setInt(1, startTime);
            pstmt.setInt(2, finishTime);
            return pstmt;
        });
    }


    public long getSenderPhasedTransactionFees(long accountId, int height) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();

             PreparedStatement pstmt = con.prepareStatement("SELECT SUM(transaction.fee) AS fees FROM transaction, phasing_poll " +
                 " LEFT JOIN phasing_poll_result ON phasing_poll.id = phasing_poll_result.id " +
                 " WHERE phasing_poll.id = transaction.id AND transaction.sender_id = ? " +
                 " AND phasing_poll_result.id IS NULL " +
                 " AND phasing_poll.finish_height > ?")) {
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getLong("fees");
            }
        }
    }

    public List<TransactionEntity> getHoldingPhasedTransactions(long holdingId, VoteWeighting.VotingModel votingModel,
                                                                long accountId, boolean withoutWhitelist, int from, int to, int height) {
        return txQueryExecutionHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* " +
                "FROM transaction, phasing_poll " +
                "WHERE phasing_poll.holding_id = ? " +
                "AND phasing_poll.voting_model = ? " +
                "AND phasing_poll.id = transaction.id " +
                "AND phasing_poll.finish_height > ? " +
                (accountId != 0 ? "AND phasing_poll.account_id = ? " : "") +
                (withoutWhitelist ? "AND phasing_poll.whitelist_size = 0 " : "") +
                "ORDER BY transaction.height DESC, transaction.transaction_index DESC " +
                DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, holdingId);
            pstmt.setByte(++i, votingModel.getCode());
            pstmt.setInt(++i, height);
            if (accountId != 0) {
                pstmt.setLong(++i, accountId);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return pstmt;
        });
    }

    public List<TransactionEntity> getAccountPhasedTransactions(long accountId, int from, int to, int height) {
        return txQueryExecutionHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, phasing_poll " +
                " LEFT JOIN phasing_poll_result ON phasing_poll.id = phasing_poll_result.id " +
                " WHERE phasing_poll.id = transaction.id AND (transaction.sender_id = ? OR transaction.recipient_id = ?) " +
                " AND phasing_poll_result.id IS NULL " +
                " AND phasing_poll.finish_height > ? ORDER BY transaction.height DESC, transaction.transaction_index DESC " +
                DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, height);
            DbUtils.setLimits(++i, pstmt, from, to);
            return pstmt;
        });
    }

    public int getAccountPhasedTransactionCount(long accountId, int height) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction, phasing_poll " +
                 " LEFT JOIN phasing_poll_result ON phasing_poll.id = phasing_poll_result.id " +
                 " WHERE phasing_poll.id = transaction.id AND (transaction.sender_id = ? OR transaction.recipient_id = ?) " +
                 " AND phasing_poll_result.id IS NULL " +
                 " AND phasing_poll.finish_height > ?")) {
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int getAllPhasedTransactionsCount() throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             @DatabaseSpecificDml(DmlMarker.NAMED_SUB_SELECT)
             PreparedStatement pstmt = con.prepareStatement(
                 "select count(*) from (select id from phasing_poll UNION select id from phasing_poll_result) as id_count")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<TransactionDbInfo> getActivePhasedTransactionDbIdsAfterHeight(int height) throws SQLException {
        List<TransactionDbInfo> excludeInfos = new ArrayList<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        int blockTimestamp = blockTimestamp(height);
        if (blockTimestamp == 0) {
            throw new IllegalStateException("Block with height " + height + " was not found or has unacceptable timestamp");
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT db_id, id FROM transaction WHERE id IN " +
                     "(SELECT id FROM phasing_poll WHERE height <= ? AND (finish_height > ? " +
                     " OR finish_height = -1) AND (finish_time > ? OR finish_time = -1))")) {
            pstmt.setInt(1, height);
            pstmt.setInt(2, height);
            pstmt.setInt(3, blockTimestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    excludeInfos.add(new TransactionDbInfo(rs.getLong("db_id"), id));
                }
            }
        }
        return excludeInfos;
    }

    public boolean isTransactionPhased(long id) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("select 1 from phasing_poll where id = ? UNION select 1 from phasing_poll_result where id = ?")) {
            pstmt.setLong(1, id);
            pstmt.setLong(2, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void trim(int height) {
        super.trim(height);
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             DbIterator<PhasingPoll> pollsToTrim = getAllFinishedPolls(height);
             PreparedStatement pstmt1 = con.prepareStatement("DELETE FROM phasing_poll WHERE id = ?");
             PreparedStatement pstmt2 = con.prepareStatement("DELETE FROM phasing_poll_voter WHERE transaction_id = ?");
             PreparedStatement pstmt3 = con.prepareStatement("DELETE FROM phasing_vote WHERE transaction_id = ?");
             PreparedStatement pstmt4 = con.prepareStatement("DELETE FROM phasing_poll_linked_transaction WHERE transaction_id = ?")) {
            while (pollsToTrim.hasNext()) {
                long id = pollsToTrim.next().getId();
                pstmt1.setLong(1, id);
                pstmt1.executeUpdate();
                pstmt2.setLong(1, id);
                pstmt2.executeUpdate();
                pstmt3.setLong(1, id);
                pstmt3.executeUpdate();
                pstmt4.setLong(1, id);
                pstmt4.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Return all finished phasings before height (exclusive, because it's required trim behavior)
     * @param height height of the blockchain before which finished polls should be searched
     * @return lazy db iterator of the resulting finished polls
     */
    private DbIterator<PhasingPoll> getAllFinishedPolls(int height) {
        Connection con = null;
        try {
            con = databaseManager.getDataSource().getConnection();
            int blockTimestamp = blockTimestamp(height - 1);
            String query = "SELECT * FROM phasing_poll WHERE finish_height < ? and finish_height <> -1";
            if (blockTimestamp != 0) {
                query += " or finish_time <= ? and finish_time <> -1";
            }
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, height);
            if (blockTimestamp != 0) {
                pstmt.setInt(2, blockTimestamp);
            }
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e);
        }
    }

    int blockTimestamp(int height) throws SQLException {
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement pstm = connection.prepareStatement("SELECT `timestamp` from block where height = ?")) {
            pstm.setInt(1, height);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        }
    }
}
