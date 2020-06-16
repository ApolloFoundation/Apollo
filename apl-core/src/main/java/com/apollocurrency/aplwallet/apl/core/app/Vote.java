/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public final class Vote {

    private static final PollService POLL_SERVICE = CDI.current().select(PollService.class).get();

    private static final LongKeyFactory<Vote> voteDbKeyFactory = new LongKeyFactory<Vote>("id") {
        @Override
        public DbKey newKey(Vote vote) {
            return vote.dbKey;
        }
    };

    private static final EntityDbTable<Vote> voteTable = new EntityDbTable<Vote>("vote", voteDbKeyFactory) {

        @Override
        public Vote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Vote(rs, dbKey);
        }

        @Override
        public void save(Connection con, Vote vote) throws SQLException {
            vote.save(con);
        }

        @Override
        public void trim(int height) {
            log.trace("Vote trim: NO_Sharding, height = {}", height);
            super.trim(height);
            try (Connection con = databaseManager.getDataSource().getConnection();
//                 DbIterator<Poll> polls = Poll.getPollsFinishingAtOrBefore(height, 0, Integer.MAX_VALUE);
                 DbIterator<Poll> polls = POLL_SERVICE.getPollsFinishingBelowHeight(height, 0, Integer.MAX_VALUE);
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM vote WHERE poll_id = ?")) {
                commonTrim(height, false, polls, pstmt);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public void trim(int height, boolean isSharding) {
            log.trace("Vote trim: isSharding={}, height = {}", isSharding, height);
            if (isSharding) {
                // trim when sharding process has been started
                super.trim(height);
                try (Connection con = databaseManager.getDataSource().getConnection();
                     // select all polls below or equal height value ('snapshot block' in our case)
                     DbIterator<Poll> polls = POLL_SERVICE.getPollsFinishingBelowHeight(height, 0, Integer.MAX_VALUE);
                     PreparedStatement pstmt = con.prepareStatement("DELETE FROM vote WHERE poll_id = ?")) {
                    commonTrim(height, true, polls, pstmt);
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            } else {
                // usual trim in other non sharding cases
                this.trim(height);
            }
        }
    };
    private final long id;
    private final DbKey dbKey;
    private final long pollId;
    private final long voterId;
    private final byte[] voteBytes;

    private Vote(Transaction transaction, MessagingVoteCasting attachment) {
        this.id = transaction.getId();
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.voteBytes = attachment.getPollVote();
    }

    private Vote(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.pollId = rs.getLong("poll_id");
        this.voterId = rs.getLong("voter_id");
        this.voteBytes = rs.getBytes("vote_bytes");
    }


    private Vote(long id, DbKey dbKey, long pollId, long voterId, byte[] voteBytes) {
        this.id = id;
        this.dbKey = dbKey;
        this.pollId = pollId;
        this.voterId = voterId;
        this.voteBytes = voteBytes;
    }

    private static void commonTrim(int height, boolean isSharding, DbIterator<Poll> polls, PreparedStatement pstmt) throws SQLException {
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

    public static int getCount() {
        return voteTable.getCount();
    }

    public static Vote getVote(long id) {
        return voteTable.get(voteDbKeyFactory.newKey(id));
    }

    public static DbIterator<Vote> getVotes(long pollId, int from, int to) {
        return voteTable.getManyBy(new DbClause.LongClause("poll_id", pollId), from, to);
    }

    public static Vote getVote(long pollId, long voterId) {
        DbClause clause = new DbClause.LongClause("poll_id", pollId).and(new DbClause.LongClause("voter_id", voterId));
        return voteTable.getBy(clause);
    }

    public static Vote addVote(Transaction transaction, MessagingVoteCasting attachment) {
        Vote vote = new Vote(transaction, attachment);
        voteTable.insert(vote);
        return vote;
    }

    public static void init() {
    }

    private void save(Connection con) throws SQLException {
        Blockchain blockchain = CDI.current().select(Blockchain.class).get();
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
            + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.pollId);
            pstmt.setLong(++i, this.voterId);
            pstmt.setBytes(++i, this.voteBytes);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getPollId() {
        return pollId;
    }

    public long getVoterId() {
        return voterId;
    }

    public byte[] getVoteBytes() {
        return voteBytes;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Vote{");
        sb.append("pollId=").append(pollId);
        sb.append(", id=").append(id);
        sb.append(", voterId=").append(voterId);
        sb.append('}');
        return sb.toString();
    }
}
