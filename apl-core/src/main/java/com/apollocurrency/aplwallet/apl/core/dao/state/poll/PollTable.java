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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.poll;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableMarkerInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
@Slf4j
public class PollTable extends EntityDbTable<Poll> implements SearchableTableMarkerInterface<Poll> {
    public static final String TABLE_NAME = "poll";
    public static final String FULL_TEXT_SEARCH_COLUMNS = "name,description";

    public static final String FINISH_HEIGHT = "finish_height";

    private static final LongKeyFactory<Poll> POLL_LONG_KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Poll poll) {
            return poll.getDbKey() == null ? newKey(poll.getId()) : poll.getDbKey();
        }
    };

    @Inject
    public PollTable(DatabaseManager databaseManager,
                     Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, POLL_LONG_KEY_FACTORY, false, FULL_TEXT_SEARCH_COLUMNS,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public Poll load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Poll(rs, dbKey);
    }

    @Override
    public void save(Connection con, Poll poll) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.SET_ARRAY)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO poll (id, account_id, "
                    + "name, description, `options`, finish_height, voting_model, min_balance, min_balance_model, "
                    + "holding_id, min_num_options, max_num_options, min_range_value, max_range_value, `timestamp`, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)
        ) {
            int i = 0;
            pstmt.setLong(++i, poll.getId());
            pstmt.setLong(++i, poll.getAccountId());
            pstmt.setString(++i, poll.getName());
            pstmt.setString(++i, poll.getDescription());
            DbUtils.setArray(pstmt, ++i, poll.getOptions());
            pstmt.setInt(++i, poll.getFinishHeight());
            pstmt.setByte(++i, poll.getVoteWeighting().getVotingModel().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, poll.getVoteWeighting().getMinBalance());
            pstmt.setByte(++i, poll.getVoteWeighting().getMinBalanceModel().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, poll.getVoteWeighting().getHoldingId());
            pstmt.setByte(++i, poll.getMinNumberOfOptions());
            pstmt.setByte(++i, poll.getMaxNumberOfOptions());
            pstmt.setByte(++i, poll.getMinRangeValue());
            pstmt.setByte(++i, poll.getMaxRangeValue());
            pstmt.setInt(++i, poll.getTimestamp());
            pstmt.setInt(++i, poll.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    poll.setDbId(rs.getLong(1));
                }
            }
        }
    }

    public Poll getPoll(long id) {
        return get(POLL_LONG_KEY_FACTORY.newKey(id));
    }

    public DbIterator<Poll> getPollsFinishingAtOrBefore(int height, int from, int to) {
        return getManyBy(new DbClause.IntClause(FINISH_HEIGHT, DbClause.Op.LTE, height), from, to);
    }

    public DbIterator<Poll> getPollsFinishingBelowHeight(int height, int from, int to) {
        // select all Polls where 'finish_height' is LESS (DbClause.Op.LT) then specified height value
        return getManyBy(new DbClause.IntClause(FINISH_HEIGHT, DbClause.Op.LT, height), from, to);
    }

    public DbIterator<Poll> getPollsFinishingAtHeight(int height) {
        // EXACT matching to Poll finish height
        return getManyBy(new DbClause.IntClause(FINISH_HEIGHT, height), 0, Integer.MAX_VALUE);
    }

    public DbIterator<Poll> getActivePolls(int from, int to, int height) {
        return getManyBy(new DbClause.IntClause(FINISH_HEIGHT, DbClause.Op.GT, height), from, to);
    }

    public DbIterator<Poll> getPollsByAccount(
        long accountId,
        boolean includeFinished,
        boolean finishedOnly,
        int from,
        int to,
        int height
    ) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (finishedOnly) {
            dbClause = dbClause.and(new DbClause.IntClause(FINISH_HEIGHT, DbClause.Op.LTE, height));
        } else if (!includeFinished) {
            dbClause = dbClause.and(new DbClause.IntClause(FINISH_HEIGHT, DbClause.Op.GT, height));
        }
        return getManyBy(dbClause, from, to);
    }

    public DbIterator<Poll> getVotedPollsByAccount(
        final long accountId,
        final int from,
        final int to
    ) throws AplException.NotValidException {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            //extract voted poll ids from attachment
            try (PreparedStatement pstmt = con.prepareStatement(
                "(SELECT attachment_bytes FROM transaction " +
                    "WHERE sender_id = ? AND type = ? AND subtype = ? " +
                    "ORDER BY block_timestamp DESC, transaction_index DESC"
                    + DbUtils.limitsClause(from, to))) {
                int i = 0;
                pstmt.setLong(++i, accountId);
                pstmt.setByte(++i, TransactionTypes.TransactionTypeSpec.VOTE_CASTING.getType());
                pstmt.setByte(++i, TransactionTypes.TransactionTypeSpec.VOTE_CASTING.getSubtype());
                DbUtils.setLimits(++i, pstmt, from, to);
                List<Long> ids = new ArrayList<>();
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        byte[] bytes = rs.getBytes("attachment_bytes");
                        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(bytes);
                        long pollId = new MessagingVoteCasting(buffer).getPollId();
                        ids.add(pollId);
                    }
                }
                try (PreparedStatement pollStatement = con.prepareStatement(
                    "SELECT * FROM poll WHERE id IN (SELECT * FROM table(x bigint = ? ))")) {
                    pollStatement.setObject(1, ids.toArray());
                    return getManyBy(con, pollStatement, false);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getCount() {
        return getCount();
    }

    public Poll addPoll(
        final Transaction transaction,
        final MessagingPollCreation attachment,
        final int timestamp,
        final int height
    ) {
        Poll poll = new Poll(transaction, attachment, POLL_LONG_KEY_FACTORY.newKey(transaction.getId()), timestamp);
        log.trace("addPoll = {}, height = {}, blockId={}", poll, transaction.getHeight(), transaction.getBlockId());
        poll.setHeight(height);
        insert(poll);
        return poll;
    }

    public DbKey getDbKey(final Poll poll) {
        return POLL_LONG_KEY_FACTORY.newKey(poll);
    }

}
