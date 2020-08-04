package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 6/12/2020
 */
@ExtendWith(MockitoExtension.class)
class PollOptionResultServiceImplTest {
    @Mock
    private BlockChainInfoService blockChainInfoService;

    @Mock
    private PollTable pollTable;

    @Mock
    private PollResultTable pollResultTable;

    @InjectMocks
    @Spy
    private PollOptionResultServiceImpl pollOptionResultService;

    @Test
    void shouldGetResultsByPollWhileFinished() {
        //GIVEN
        final int height = 1440;
        when(blockChainInfoService.getHeight()).thenReturn(height);
        final Poll poll = mock(Poll.class);
        final int finishHeight = 500;
        when(poll.getFinishHeight()).thenReturn(finishHeight);
        final DbKey dbKey = mock(DbKey.class);
        when(pollTable.getDbKey(poll)).thenReturn(dbKey);
        final PollOptionResult pollOptionResult = mock(PollOptionResult.class);
        final List<PollOptionResult> expectedPollOptionResult = List.of(pollOptionResult);
        when(pollResultTable.get(dbKey)).thenReturn(expectedPollOptionResult);

        //WHEN
        final List<PollOptionResult> actualPollOptionResult =
            pollOptionResultService.getResultsByPoll(poll);

        //THEN
        assertEquals(expectedPollOptionResult, actualPollOptionResult);
    }

    @Test
    void shouldGetResultsByPollWhileNotFinished() {
        //GIVEN
        final int height = 1440;
        when(blockChainInfoService.getHeight()).thenReturn(height);
        final Poll poll = mock(Poll.class);
        final int finishHeight = 5000;
        when(poll.getFinishHeight()).thenReturn(finishHeight);
        final VoteWeighting voteWeighting = mock(VoteWeighting.class);
        final String[] options = new String[]{"op"};
        when(poll.getOptions()).thenReturn(options);
        when(poll.getVoteWeighting()).thenReturn(voteWeighting);
        final long id = 1L;
        final long accountId = 2L;
        final int optionsLength = 1;
        when(poll.getId()).thenReturn(id);
        when(poll.getAccountId()).thenReturn(accountId);
        final List<PollOptionResult> pollOptionResults = List.of(mock(PollOptionResult.class));
        doReturn(pollOptionResults)
            .when(pollOptionResultService)
            .countResults(voteWeighting, height, id, accountId, optionsLength);

        //WHEN
        final List<PollOptionResult> actualPollOptionResult =
            pollOptionResultService.getResultsByPoll(poll);

        //THEN
        assertEquals(pollOptionResults, actualPollOptionResult);
    }
}