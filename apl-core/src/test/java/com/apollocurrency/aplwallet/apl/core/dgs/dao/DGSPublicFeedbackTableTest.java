/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class DGSPublicFeedbackTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DGSPublicFeedbackTable.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();
    @Inject
    DGSPublicFeedbackTable table;

    DGSTestData dtd;



    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
    }

    @Test
    void testGetByPurchaseId() {
        List<DGSPublicFeedback> feedbacks = table.get(dtd.PUBLIC_FEEDBACK_12.getId());

        assertEquals(List.of(dtd.PUBLIC_FEEDBACK_11, dtd.PUBLIC_FEEDBACK_12, dtd.PUBLIC_FEEDBACK_13), feedbacks);
    }

    @Test
    void testGetDeletedByPurchaseId() {
        List<DGSPublicFeedback> feedbacks = table.get(dtd.PUBLIC_FEEDBACK_8.getId());

        assertEquals(0, feedbacks.size());
    }

    @Test
    void testNonexistentById() {
        List<DGSPublicFeedback> feedbacks = table.get(-1);
        assertEquals(0, feedbacks.size());
    }

}
