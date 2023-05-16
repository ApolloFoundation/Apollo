/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;


@Tag("slow")
public class PhasingResultTableTest extends EntityDbTableTest<PhasingPollResult> {
    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/phasing-poll-data.sql", "db/schema.sql");

    PhasingPollResultTable table;
    PhasingTestData ptd;
    TransactionTestData ttd;


    public PhasingResultTableTest() {
        super(PhasingPollResult.class);
    }


    @BeforeEach
    @Override
    public void setUp() {
        table = new PhasingPollResultTable(extension.getDatabaseManager(), mock(Event.class));
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
        super.setUp();
    }

    @Override
    public PhasingPollResult valueToInsert() {
        return ptd.NEW_RESULT;
    }

    @Override
    public DerivedDbTable<PhasingPollResult> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<PhasingPollResult> getAll() {
        return new ArrayList<>(List.of(ptd.SHARD_RESULT_0, ptd.RESULT_0, ptd.RESULT_1, ptd.RESULT_2, ptd.RESULT_3));
    }

    @Override
    @Test
    public void testInsertAlreadyExist() {
        PhasingPollResult value = ptd.RESULT_1;
        Assertions.assertThrows(RuntimeException.class, () -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            table.insert(value);
        }));
    }
}
