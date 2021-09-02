/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.VoteTable;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 6/12/2020
 */
@ExtendWith(MockitoExtension.class)
class PollServiceImplTest {
    @SuppressWarnings("unchecked")
    private final IteratorToStreamConverter<Poll> converter = mock(IteratorToStreamConverter.class);

    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private PollTable pollTable;

    @Mock
    private PollResultTable pollResultTable;

    @Mock
    private PollOptionResultService pollOptionResultService;

    @Mock
    private VoteTable voteTable;

    @Mock
    private BlockchainImpl blockchain;
    @Mock
    private FullTextSearchUpdater fullTextSearchUpdater;
    @Mock
    private FullTextSearchService fullTextSearchService;

    private PollServiceImpl pollService;

    @BeforeEach
    void setUp() {
        this.pollService = new PollServiceImpl(
            blockChainInfoService,
            pollTable,
            pollResultTable,
            converter,
            pollOptionResultService,
            voteTable,
            blockchain,
            fullTextSearchUpdater,
            fullTextSearchService
        );
    }

    @Test
    void shouldCheckPolls() {
        //GIVEN
        int currentHeight = 33000;
        @SuppressWarnings("unchecked") final DbIterator<Poll> dbIterator = mock(DbIterator.class);
        when(pollTable.getPollsFinishingAtHeight(currentHeight))
            .thenReturn(dbIterator);
        @SuppressWarnings("unchecked") final Iterator<Poll> pollIterator = mock(Iterator.class);
        when(dbIterator.iterator()).thenReturn(pollIterator);
        when(pollIterator.hasNext()).thenReturn(true, false);
        final Poll poll = mock(Poll.class);
        when(pollIterator.next()).thenReturn(poll);
        final VoteWeighting voteWeighting = mock(VoteWeighting.class);
        final String[] options = new String[]{"op"};
        when(poll.getOptions()).thenReturn(options);
        when(poll.getVoteWeighting()).thenReturn(voteWeighting);
        final long id = 1L;
        final long accountId = 2L;
        final int optionsLength = 1;
        when(poll.getId()).thenReturn(id);
        when(poll.getAccountId()).thenReturn(accountId);
        final PollOptionResult pollOptionResult = mock(PollOptionResult.class);
        final List<PollOptionResult> pollOptionResults = List.of(pollOptionResult);
        when(pollOptionResultService.countResults(voteWeighting, currentHeight, id, accountId, optionsLength))
            .thenReturn(pollOptionResults);

        //WHEN
        pollService.checkPolls(currentHeight);

        //THEN
        verify(pollResultTable).insert(pollOptionResults);
    }

    @Test
    void shouldGetPoll() {
        //GIVEN
        final long id = 1L;
        final Poll poll = mock(Poll.class);
        when(pollTable.getPoll(id)).thenReturn(poll);

        //WHEN
        final Poll actualPoll = pollService.getPoll(id);

        //THEN
        assertEquals(poll, actualPoll);
    }

    @Test
    void shouldGetPollsFinishingAtOrBefore() {
        //GIVEN
        final int height = 1055;
        final int from = 10;
        final int to = 50;
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        when(pollTable.getPollsFinishingAtOrBefore(height, from, to))
            .thenReturn(pollIterator);
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);

        //WHEN
        final Stream<Poll> actualPolls = pollService.getPollsFinishingAtOrBefore(height, from, to);

        //THEN
        assertEquals(polls, actualPolls);
    }

    @Test
    void shouldGetAllPolls() {
        //GIVEN
        final int from = 10;
        final int to = 50;
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        when(pollTable.getAll(from, to)).thenReturn(pollIterator);
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);

        //WHEN
        final Stream<Poll> actualPolls = pollService.getAllPolls(from, to);

        //THEN
        assertEquals(polls, actualPolls);
    }

    @Test
    void shouldGetActivePolls() {
        //GIVEN
        final int from = 10;
        final int to = 50;
        final int height = 1440;
        when(blockChainInfoService.getHeight()).thenReturn(height);
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        when(pollTable.getActivePolls(from, to, height)).thenReturn(pollIterator);
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);

        //WHEN
        final Stream<Poll> actualPolls = pollService.getActivePolls(from, to);

        //THEN
        assertEquals(polls, actualPolls);
    }

    @Test
    void shouldGetPollsByAccount() {
        //GIVEN
        final int from = 10;
        final int to = 50;
        final long accountId = 5L;
        final boolean includeFinished = true;
        final boolean finishedOnly = true;
        final int height = 1440;
        when(blockChainInfoService.getHeight()).thenReturn(height);
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        when(pollTable.getPollsByAccount(accountId, includeFinished, finishedOnly, from, to, height))
            .thenReturn(pollIterator);
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);

        //WHEN
        final Stream<Poll> actualPolls =
            pollService.getPollsByAccount(accountId, includeFinished, finishedOnly, from, to);

        //THEN
        assertEquals(polls, actualPolls);
    }

    @Test
    void shouldGetVotedPollsByAccount() throws AplException.NotValidException {
        //GIVEN
        final int from = 10;
        final int to = 50;
        final long accountId = 5L;
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        when(pollTable.getVotedPollsByAccount(accountId, from, to))
            .thenReturn(pollIterator);
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);

        //WHEN
        final Stream<Poll> actualPolls = pollService.getVotedPollsByAccount(accountId, from, to);

        //THEN
        assertEquals(polls, actualPolls);
    }

    @Test
    void searchPolls() throws SQLException {
        //GIVEN
        final int from = 10;
        final int to = 50;
        final String query = "query";
        final boolean includeFinished = true;
        final int height = 1440;
        @SuppressWarnings("unchecked") final DbIterator<Poll> pollIterator = mock(DbIterator.class);
        doReturn("poll").when(pollTable).getTableName();
        @SuppressWarnings("unchecked") final Stream<Poll> polls = mock(Stream.class);
        when(converter.convert(pollIterator)).thenReturn(polls);
        ResultSet rs = mock(ResultSet.class);
        doReturn(rs).when(fullTextSearchService).search("public", "poll", query, Integer.MAX_VALUE, 0);

        //WHEN
        final Stream<Poll> actualPolls = pollService.searchPolls(query, includeFinished, from, to);
        assertNotNull(actualPolls);

        //THEN
        verify(fullTextSearchService).search("public", "poll", query, Integer.MAX_VALUE, 0);
    }

    @Test
    void getCount() {
        //GIVEN
        final int count = 1;
        when(pollTable.getCount()).thenReturn(count);

        //WHEN
        final int actualCount = pollService.getCount();

        //THEN
        assertEquals(count, actualCount);
    }

    @Test
    void shouldAddPoll() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final MessagingPollCreation attachment = mock(MessagingPollCreation.class);
        final int timestamp = 1;
        when(blockChainInfoService.getLastBlockTimestamp()).thenReturn(timestamp);
        final int height = 1000;
        when(blockChainInfoService.getHeight()).thenReturn(height);
        doReturn("poll").when(pollTable).getTableName();
        Poll poll = mock(Poll.class);
        doReturn(10L).when(poll).getDbId();
        doReturn(poll).when(pollTable).addPoll(transaction, attachment, timestamp, height);

        //WHEN
        pollService.addPoll(transaction, attachment);

        //THEN
        verify(pollTable).addPoll(transaction, attachment, timestamp, height);
        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
    }

    @ParameterizedTest
    @CsvSource({"400, false", "1000, true", "500, true"})
    void shouldTestIsFinished(final int height, final boolean expected) {
        //GIVEN
        final int finishHeight = 500;
        when(blockChainInfoService.getHeight()).thenReturn(height);

        //WHEN
        final boolean actual = pollService.isFinished(finishHeight);

        //THEN
        assertEquals(expected, actual);
    }
}