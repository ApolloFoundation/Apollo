/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingPollVoterTable extends ValuesDbTable<PhasingPoll, Long> {
    private static final String TABLE_NAME = "phasing_poll_voter";
    private static final LongKeyFactory<PhasingPoll> KEY_FACTORY = new LongKeyFactory<PhasingPoll>("transaction_id") {
        @Override
        public DbKey newKey(PhasingPoll poll) {
            return new LongKey(poll.getId());
        }
    };
    private final Blockchain blockchain;


    @Inject
    public PhasingPollVoterTable(Blockchain blockchain) {
        super(TABLE_NAME, KEY_FACTORY);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
    }

    public List<Long> get(long pollId) {
        return get(KEY_FACTORY.newKey(pollId));
    }

    @Override
    protected Long load(Connection con, ResultSet rs) throws SQLException {
        return rs.getLong("voter_id");
    }

    @Override
    protected void save(Connection con, PhasingPoll poll, Long accountId) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_voter (transaction_id, "
                + "voter_id, height) VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, poll.getId());
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DbIterator<Transaction> getVoterPhasedTransactions(long voterId, int from, int to) throws SQLException {
        Connection con = null;
        try {
            con = getDatabaseManager().getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* "
                    + "FROM transaction, phasing_poll_voter, phasing_poll "
                    + "LEFT JOIN phasing_poll_result ON phasing_poll.id = phasing_poll_result.id "
                    + "WHERE transaction.id = phasing_poll.id AND "
                    + "phasing_poll.finish_height > ? AND "
                    + "phasing_poll.id = phasing_poll_voter.transaction_id "
                    + "AND phasing_poll_voter.voter_id = ? "
                    + "AND phasing_poll_result.id IS NULL "
                    + "ORDER BY transaction.height DESC, transaction.transaction_index DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.setLong(++i, voterId);
            DbUtils.setLimits(++i, pstmt, from, to);

            return blockchain.getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw e;
        }
    }
}
