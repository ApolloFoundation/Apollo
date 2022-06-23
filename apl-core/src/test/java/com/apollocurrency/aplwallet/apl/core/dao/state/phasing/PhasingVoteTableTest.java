/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
class PhasingVoteTableTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);

    PhasingVoteTable table;
    PhasingTestData ptd;

    @BeforeEach
    void setUp() {
        table = new PhasingVoteTable(extension.getDatabaseManager(), mock(Event.class));
        ptd = new PhasingTestData();
    }

    @Test
    void testGetByPhasingIdAndVoterId() {
        PhasingVote phasingVote = table.get(ptd.POLL_1_VOTE_0.getPhasedTransactionId(), ptd.POLL_1_VOTE_0.getVoterId());

        assertEquals(ptd.POLL_1_VOTE_0, phasingVote);
    }

    @Test
    void getByPhasingId() {
        List<PhasingVote> phasingVotes = table.get(ptd.POLL_1_VOTE_1.getPhasedTransactionId());

        assertEquals(phasingVotes, List.of(ptd.POLL_1_VOTE_1, ptd.POLL_1_VOTE_0));
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> table.insert(ptd.NEW_VOTE));

        List<PhasingVote> phasingVotes = table.get(ptd.POLL_1.getId());
        assertEquals(phasingVotes, List.of(ptd.POLL_1_VOTE_1, ptd.POLL_1_VOTE_0, ptd.NEW_VOTE));
    }
}