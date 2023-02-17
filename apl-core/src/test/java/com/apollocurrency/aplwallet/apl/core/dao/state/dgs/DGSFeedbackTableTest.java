/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;


@Tag("slow")
public class DGSFeedbackTableTest extends ValuesDbTableTest<DGSFeedback> {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/dgs-data.sql", "db/schema.sql");

    DGSFeedbackTable table;

    DGSTestData dtd;

    public DGSFeedbackTableTest() {
        super(DGSFeedback.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        table = new DGSFeedbackTable(extension.getDatabaseManager(), mock(Event.class));
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    protected List<DGSFeedback> dataToInsert() {
        return List.of(dtd.NEW_FEEDBACK_0, dtd.NEW_FEEDBACK_1, dtd.NEW_FEEDBACK_2);
    }

    @Override
    public DerivedDbTable<DGSFeedback> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSFeedback> getAll() {
        return List.of(dtd.FEEDBACK_0, dtd.FEEDBACK_1, dtd.FEEDBACK_2, dtd.FEEDBACK_3, dtd.FEEDBACK_4, dtd.FEEDBACK_5, dtd.FEEDBACK_6, dtd.FEEDBACK_7, dtd.FEEDBACK_8, dtd.FEEDBACK_9, dtd.FEEDBACK_10, dtd.FEEDBACK_11);
    }

    @Override
    protected List<DGSFeedback> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }
}
