/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


@Tag("slow")
public class DGSPublicFeedbackTableTest extends ValuesDbTableTest<DGSPublicFeedback> {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/dgs-data.sql", "db/schema.sql");

    DGSPublicFeedbackTable table;

    DGSTestData dtd;

    public DGSPublicFeedbackTableTest() {
        super(DGSPublicFeedback.class);
    }


    @BeforeEach
    @Override
    public void setUp() {
        table = new DGSPublicFeedbackTable(extension.getDatabaseManager(), mock(Event.class));
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    protected List<DGSPublicFeedback> dataToInsert() {
        return List.of(dtd.NEW_PUBLIC_FEEDBACK_0, dtd.NEW_PUBLIC_FEEDBACK_1, dtd.NEW_PUBLIC_FEEDBACK_2);
    }

    @Override
    public DerivedDbTable<DGSPublicFeedback> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSPublicFeedback> getAll() {
        return new ArrayList<>(List.of(dtd.PUBLIC_FEEDBACK_0, dtd.PUBLIC_FEEDBACK_1, dtd.PUBLIC_FEEDBACK_2, dtd.PUBLIC_FEEDBACK_3, dtd.PUBLIC_FEEDBACK_4, dtd.PUBLIC_FEEDBACK_5, dtd.PUBLIC_FEEDBACK_6, dtd.PUBLIC_FEEDBACK_7, dtd.PUBLIC_FEEDBACK_8, dtd.PUBLIC_FEEDBACK_9, dtd.PUBLIC_FEEDBACK_10, dtd.PUBLIC_FEEDBACK_11, dtd.PUBLIC_FEEDBACK_12, dtd.PUBLIC_FEEDBACK_13));
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

    @Override
    protected List<DGSPublicFeedback> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }

}
