/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.phasing.PhasingPollVoterMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Singleton
public class PhasingPollVoterTable extends ValuesDbTable<PhasingPollVoter> {
    private static final String TABLE_NAME = "phasing_poll_voter";
    private static final LongKeyFactory<PhasingPollVoter> KEY_FACTORY = new LongKeyFactory<PhasingPollVoter>("transaction_id") {
        @Override
        public DbKey newKey(PhasingPollVoter poll) {
            if (poll.getDbKey() == null) {
                poll.setDbKey(new LongKey(poll.getPollId()));
            }
            return poll.getDbKey();
        }
    };
    private static final PhasingPollVoterMapper MAPPER = new PhasingPollVoterMapper(KEY_FACTORY);
    private final Blockchain blockchain;


    @Inject
    public PhasingPollVoterTable(Blockchain blockchain) {
        super(TABLE_NAME, false, KEY_FACTORY, false);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
    }

    public List<PhasingPollVoter> get(long pollId) {
        return get(KEY_FACTORY.newKey(pollId));
    }

    @Override
    public PhasingPollVoter load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public void save(Connection con, PhasingPollVoter voter) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_voter (transaction_id, "
            + "voter_id, height) VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, voter.getPollId());
            pstmt.setLong(++i, voter.getVoterId());
            pstmt.setInt(++i, voter.getHeight());
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
