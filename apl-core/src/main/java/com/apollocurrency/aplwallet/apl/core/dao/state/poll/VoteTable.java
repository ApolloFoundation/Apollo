/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.poll;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.Vote;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
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
    public static final LongKeyFactory<Vote> voteDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Vote vote) {
            return vote.getDbKey();
        }
    };
    private PollTable pollTable;

    @Inject
    public VoteTable(PollTable pollTable,
                     DerivedTablesRegistry derivedDbTablesRegistry,
                     DatabaseManager databaseManager,
                     Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("vote", voteDbKeyFactory, false, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
        this.pollTable = pollTable;
    }


    @Override
    public void save(Connection con, Vote entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
            + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, entity.getId());
            pstmt.setLong(++i, entity.getPollId());
            pstmt.setLong(++i, entity.getVoterId());
            pstmt.setBytes(++i, entity.getVoteBytes());
            pstmt.setInt(++i, entity.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected Vote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Vote(rs, dbKey);
    }

    @Override
    public void trim(int height, boolean isSharding) {
        log.trace("Vote trim: NO_Sharding, height = {}", height);
        super.trim(height, isSharding);
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

    public Vote addVote(Transaction transaction, MessagingVoteCasting attachment, int height) {
        Vote vote = new Vote(transaction, attachment, height);
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

}
