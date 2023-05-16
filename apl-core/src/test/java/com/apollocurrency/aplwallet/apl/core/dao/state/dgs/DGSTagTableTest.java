/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;


@Tag("slow")
public class DGSTagTableTest extends EntityDbTableTest<DGSTag> {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/dgs-data.sql", "db/schema.sql");

    DGSTagTable table;
    DGSTestData dtd;

    public DGSTagTableTest() {
        super(DGSTag.class);
    }


    @BeforeEach
    @Override
    public void setUp() {
        table = new DGSTagTable(extension.getDatabaseManager(), mock(Event.class));
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    public DGSTag valueToInsert() {
        return dtd.NEW_TAG;
    }

    @Override
    public DerivedDbTable<DGSTag> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSTag> getAll() {
        return new ArrayList<>(List.of(dtd.TAG_0, dtd.TAG_1, dtd.TAG_2, dtd.TAG_3, dtd.TAG_4, dtd.TAG_5, dtd.TAG_6, dtd.TAG_7, dtd.TAG_8, dtd.TAG_9, dtd.TAG_10, dtd.TAG_11, dtd.TAG_12));
    }

    @Override
    public Comparator<DGSTag> getDefaultComparator() {
        return Comparator.comparing(DGSTag::getInStockCount).thenComparing(DGSTag::getTotalCount).reversed().thenComparing(DGSTag::getTag);
    }

    @Test
    void testGetByTag() {
        DGSTag dgsTag = table.get(dtd.TAG_10.getTag());

        assertEquals(dtd.TAG_10, dgsTag);
    }

    @Test
    void testGetDeletedTag() {

        DGSTag dgsTag = table.get(dtd.TAG_8.getTag());

        assertNull(dgsTag);
    }

    @Test
    void testGetByNonexistentTag() {
        DGSTag dgsTag = table.get("");
        assertNull(dgsTag);
    }

    @Override
    public List<DGSTag> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }
}
