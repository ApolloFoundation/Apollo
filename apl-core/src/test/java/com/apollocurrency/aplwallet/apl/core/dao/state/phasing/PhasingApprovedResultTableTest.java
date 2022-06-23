/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingApprovedResultTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
class PhasingApprovedResultTableTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/phasing-poll-data.sql", "db/schema.sql");

    PhasingApprovedResultTable table;
    private PhasingApprovedResultTestData data;

    @BeforeEach
    void setUp() {
        table = new PhasingApprovedResultTable(extension.getDatabaseManager(), mock(Event.class));
        data = new PhasingApprovedResultTestData();
    }

    @Test
    void testInsertNew() {
        DbUtils.inTransaction(extension, (con) -> table.insert(data.NEW_RESULT));
        PhasingApprovalResult phasingApprovalResult = table.get(data.NEW_RESULT.getPhasingTx());
        assertEquals(data.NEW_RESULT, phasingApprovalResult);
    }

    @Test
    void testGetById() {
        PhasingApprovalResult result = table.get(data.RESULT_1.getPhasingTx());

        assertEquals(data.RESULT_1, result);
    }
}