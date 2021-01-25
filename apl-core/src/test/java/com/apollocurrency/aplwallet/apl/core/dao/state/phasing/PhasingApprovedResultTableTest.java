/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.data.PhasingApprovedResultTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j

@Tag("slow")
@QuarkusTest
class PhasingApprovedResultTableTest extends DbContainerBaseTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        FullTextConfigImpl.class,
        PhasingApprovedResultTable.class,
        DerivedDbTablesRegistryImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .build();*/
    @Inject
    PhasingApprovedResultTable table;
    private PhasingApprovedResultTestData data;

    @BeforeEach
    void setUp() {
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