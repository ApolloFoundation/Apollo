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

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import static com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable.FINISH_HEIGHT;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.VoteTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.Vote;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollOptionResultService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class PollServiceImpl implements PollService {
    private final BlockChainInfoService blockChainInfoService;
    private final PollTable pollTable;
    private final PollResultTable pollResultTable;
    private final IteratorToStreamConverter<Poll> converter;
    private final PollOptionResultService pollOptionResultService;
    private final VoteTable voteTable;
    private final BlockchainImpl blockchain;
    private final FullTextSearchUpdater fullTextSearchUpdater;
    private final FullTextSearchService fullTextSearchService;

    /**
     * Constructor for unit tests.
     *
     * @param blockChainInfoService
     * @param pollTable
     * @param pollResultTable
     * @param converter
     * @param pollOptionResultService
     */
    public PollServiceImpl(
        final BlockChainInfoService blockChainInfoService,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final IteratorToStreamConverter<Poll> converter,
        final PollOptionResultService pollOptionResultService,
        final VoteTable voteTable,
        BlockchainImpl blockchain,
        FullTextSearchUpdater fullTextSearchUpdater,
        FullTextSearchService fullTextSearchService
    ) {
        this.blockChainInfoService = blockChainInfoService;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = converter;
        this.pollOptionResultService = pollOptionResultService;
        this.voteTable = voteTable;
        this.blockchain = blockchain;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Inject
    public PollServiceImpl(
        final BlockChainInfoService blockChainInfoService,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final PollOptionResultService pollOptionResultService,
        final VoteTable voteTable,
        final BlockchainImpl blockchain,
        final FullTextSearchUpdater fullTextSearchUpdater,
        final FullTextSearchService fullTextSearchService
        ) {
        this.blockChainInfoService = blockChainInfoService;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = new IteratorToStreamConverter<>();
        this.pollOptionResultService = pollOptionResultService;
        this.voteTable = voteTable;
        this.blockchain = blockchain;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Override
    public void checkPolls(int currentHeight) {
        // select all Polls where 'finish_height' is EQUAL (DbClause.Op.EQ) then specified height value
        try (DbIterator<Poll> polls = pollTable.getPollsFinishingAtHeight(currentHeight)) {
            int index = 0;
            for (Poll poll : polls) {
                try {
                    List<PollOptionResult> results = pollOptionResultService.countResults(
                        poll.getVoteWeighting(),
                        currentHeight,
                        poll.getId(),
                        poll.getAccountId(),
                        poll.getOptions().length
                    );
                    log.trace("checkPolls: height = {}, [{}] PollId = {} has = {}", currentHeight, index, poll.getId(), results.size());
                    pollResultTable.insert(results);
                    log.trace("checkPolls: height = {}, [{}] PollId = {} checked : {}", currentHeight, index, poll.getId(), results);
                    index++;
                } catch (RuntimeException e) {
                    log.error("Couldn't count RollResult for poll {} at height = {}", poll.getId(), currentHeight, e);
                }
            }
        }
    }

    @Override
    public Poll getPoll(long id) {
        return pollTable.getPoll(id);
    }

    @Override
    public Stream<Poll> getPollsFinishingAtOrBefore(int height, int from, int to) {
        return converter.convert(pollTable.getPollsFinishingAtOrBefore(height, from, to));
    }

    @Override
    public Stream<Poll> getAllPolls(int from, int to) {
        return converter.convert(pollTable.getAll(from, to));
    }

    @Override
    public Stream<Poll> getActivePolls(int from, int to) {
        return converter.convert(pollTable.getActivePolls(from, to, blockChainInfoService.getHeight()));
    }

    @Override
    public Stream<Poll> getPollsByAccount(long accountId, boolean includeFinished, boolean finishedOnly, int from, int to) {
        return converter.convert(
            pollTable.getPollsByAccount(accountId, includeFinished, finishedOnly, from, to, blockChainInfoService.getHeight())
        );
    }

    @Override
    public Stream<Poll> getVotedPollsByAccount(long accountId, int from, int to) throws AplException.NotValidException {
        return converter.convert(
            pollTable.getVotedPollsByAccount(accountId, from, to)
        );
    }

    @Override
    public Stream<Poll> searchPolls(String query, boolean includeFinished, int from, int to) {
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return Stream.of();
        }
        DbClause dbClause = includeFinished ? DbClause.EMPTY_CLAUSE : new DbClause.IntClause(FINISH_HEIGHT,
            DbClause.Op.GT, blockChainInfoService.getHeight());
        String sort = " ORDER BY poll.height DESC, poll.db_id DESC ";
        return converter.convert(
            fetchPollByParams(from, to, inRangeClause, dbClause, sort)
//            pollTable.searchPolls(query, includeFinished, from, to, blockChainInfoService.getHeight())
        );
    }

    @Override
    public int getCount() {
        return pollTable.getCount();
    }

    @Override
    public int getPollVoteCount() {
        return voteTable.getCount();
    }

    @Override
    public Stream<Vote> getVotes(long pollId, int from, int to) {
        return voteTable.getVotes(pollId, from, to).stream();
    }

    @Override
    public Vote getVote(long pollId, long voterId) {
        return voteTable.getVote(pollId, voterId);
    }

    @Override
    public Vote addVote(Transaction transaction, MessagingVoteCasting attachment) {
        return voteTable.addVote(transaction, attachment, blockchain.getLastBlock().getHeight());
    }

    @Override
    public void addPoll(Transaction transaction, MessagingPollCreation attachment) {
        Poll poll = pollTable.addPoll(transaction, attachment,
            blockChainInfoService.getLastBlockTimestamp(), blockChainInfoService.getHeight());
        createAndFireFullTextSearchDataEvent(poll, FullTextOperationData.OperationType.INSERT_UPDATE);
    }

    @Override
    public boolean isFinished(final int finishHeight) {
        return finishHeight <= blockChainInfoService.getHeight();
    }

    /**
     * compose db_id list for in (id,..id) SQL luceneQuery
     * @param luceneQuery lucene language luceneQuery pattern
     * @return composed sql luceneQuery part
     */
    private StringBuffer createDbIdInRangeFromLuceneData(String luceneQuery) {
        Objects.requireNonNull(luceneQuery, "luceneQuery is empty");
        StringBuffer inRange = new StringBuffer("(");
        int index = 0;
        try {
            ResultSet rs = fullTextSearchService.search("public", pollTable.getTableName(), luceneQuery, Integer.MAX_VALUE, 0);
            while (rs.next()) {
                Long DB_ID = rs.getLong(5);
                if (index == 0) {
                    inRange.append(DB_ID);
                } else {
                    inRange.append(",").append(DB_ID);
                }
                index++;
            }
            inRange.append(")");
            log.debug("{}", inRange.toString());
        } catch (SQLException e) {
            log.error("FTS failed", e);
            throw new RuntimeException(e);
        }
        return inRange;
    }

    public DbIterator<Poll> fetchPollByParams(int from, int to,
                                                      StringBuffer inRangeClause,
                                                      DbClause dbClause,
                                                      String sort) {
        Objects.requireNonNull(inRangeClause, "inRangeClause is NULL");
        Objects.requireNonNull(dbClause, "dbClause is NULL");
        Objects.requireNonNull(sort, "sort is NULL");

        Connection con = null;
        TransactionalDataSource dataSource = pollTable.getDatabaseManager().getDataSource();
        final boolean doCache = dataSource.isInTransaction();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement(
                // select and load full entities from mariadb using prefetched DB_ID list from lucene
                "SELECT " + pollTable.getTableName() + ".* FROM " + pollTable.getTableName()
                    + " WHERE " + pollTable.getTableName() + ".db_id in " + inRangeClause.toString()
                    + (pollTable.isMultiversion() ? " AND " + pollTable.getTableName() + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            DbUtils.setLimits(i, pstmt, from, to);
            return new DbIterator<>(con, pstmt, (connection, rs) -> {
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = pollTable.getDbKeyFactory().newKey(rs);
                }
                return pollTable.load(connection, rs, dbKey);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void createAndFireFullTextSearchDataEvent(Poll poll, FullTextOperationData.OperationType operationType) {
        FullTextOperationData operationData = new FullTextOperationData(
            pollTable.getTableName() + ";DB_ID;" + poll.getDbId(), pollTable.getTableName());
        operationData.setThread(Thread.currentThread().getName());
        // put relevant data into Event instance
        operationData.setOperationType(operationType);
        operationData.addColumnData(poll.getName()).addColumnData(poll.getDescription());
        // send data into Lucene index component
        log.trace("Put lucene index update data = {}", operationData);
        fullTextSearchUpdater.putFullTextOperationData(operationData);
    }

}
