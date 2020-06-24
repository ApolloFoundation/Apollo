package com.apollocurrency.aplwallet.apl.core.dao.state.poll;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Singleton
public class VoteTable extends EntityDbTable<Vote> {
    private static final LongKeyFactory<Vote> VOTE_LONG_KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Vote vote) {
            return vote.getDbKey();
        }
    };
    private PollTable pollTable;

    @Inject
    public VoteTable(PollTable pollTable) {
        super("vote", VOTE_LONG_KEY_FACTORY);
        this.pollTable = pollTable;
    }


    @Override
    public void save(Connection con, Vote entity) throws SQLException {
        Blockchain blockchain = CDI.current().select(Blockchain.class).get();
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
            + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getId());
            pstmt.setLong(++i, entity.getPollId());
            pstmt.setLong(++i, entity.getVoterId());
            pstmt.setBytes(++i, entity.getVoteBytes());
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected Vote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return map(rs, dbKey);
    }

    @Override
    public void trim(int height) {
        log.trace("Vote trim: NO_Sharding, height = {}", height);
        super.trim(height);
        try (Connection con = databaseManager.getDataSource().getConnection();
             DbIterator<Poll> polls = pollTable.getPollsFinishingBelowHeight(height, 0, Integer.MAX_VALUE);
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM vote WHERE poll_id = ?")) {
             commonTrim(height, false, polls, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<Vote> getVotes(long pollId, int from, int to) {
        return CollectionUtil.toList(getManyBy(new DbClause.LongClause("poll_id", pollId), from, to));
    }

    public Vote getVote(long pollId, long voterId) {
        DbClause clause = new DbClause.LongClause("poll_id", pollId).and(new DbClause.LongClause("voter_id", voterId));
        return getBy(clause);
    }

    public Vote addVote(Transaction transaction, MessagingVoteCasting attachment) {
        Vote vote = new Vote(transaction, attachment, VOTE_LONG_KEY_FACTORY.newKey(transaction.getId()));
        insert(vote);
        return vote;
    }


    private void commonTrim(int height, boolean isSharding, DbIterator<Poll> polls, PreparedStatement pstmt) throws SQLException {
        log.trace("Vote trim common: isSharding={}, height = {}", isSharding, height);
        int index = 0; // index for affected Polls
        int totalDeletedVotes = 0; // total number deleted Vote records from all affected Polls
        for (Poll poll : polls) {
            pstmt.setLong(1, poll.getId());
            log.trace("Vote trim common: Before deleting votes, index=[{}] by pollId={} at height = {}", index, poll.getId(), height);
            int deletedRecords = pstmt.executeUpdate();
            if (deletedRecords > 0) {
                log.trace("Vote trim common: deleted [{}] votes, index=[{}] by pollId = {}, poll finishHeight={} at blockchain height={}",
                    deletedRecords, index, poll.getId(), poll.getFinishHeight(), height);
                totalDeletedVotes += deletedRecords;
            }
            index++;
        }
        log.trace("Vote trim common: REMOVED totally [{}] votes within [{}] polls at height = {} (isSharding={})",
            totalDeletedVotes, index, height, isSharding);
    }

    private Vote map(ResultSet rs, DbKey dbKey){
        try {
            Vote vote = new Vote(rs.getLong("id"),
                dbKey,
                rs.getLong("poll_id"),
                rs.getLong("voter_id"),
                rs.getBytes("vote_bytes"));
            return vote;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
