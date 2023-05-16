/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


@Tag("slow")
public class PhasingPollVoterTableTest extends ValuesDbTableTest<PhasingPollVoter> {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/phasing-poll-data.sql", "db/schema.sql");

    PhasingPollVoterTable table;
    PhasingTestData ptd;
    TransactionTestData ttd;

    public PhasingPollVoterTableTest() {
        super(PhasingPollVoter.class);
    }


    @BeforeEach
    @Override
    public void setUp() {
        table = new PhasingPollVoterTable(new TransactionEntityRowMapper(), extension.getDatabaseManager(), mock(Event.class));
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
        super.setUp();
    }

    @Override
    public DerivedDbTable<PhasingPollVoter> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<PhasingPollVoter> getAll() {
        return List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1, ptd.POLL_4_VOTER_0, ptd.FAKE_VOTER_0, ptd.FAKE_VOTER_1, ptd.FAKE_VOTER_2, ptd.POLL_5_VOTER_0, ptd.POLL_5_VOTER_1);
    }

    @Override
    protected List<PhasingPollVoter> dataToInsert() {
        return List.of(ptd.NEW_VOTER_0, ptd.NEW_VOTER_1);
    }


    @Test
    void testGetVotersForPollWithoutVoters() {
        List<PhasingPollVoter> pollVoters = table.get(ptd.POLL_2.getId());

        assertTrue(pollVoters.isEmpty(), "Poll voters should not exist for poll2");
    }

}
