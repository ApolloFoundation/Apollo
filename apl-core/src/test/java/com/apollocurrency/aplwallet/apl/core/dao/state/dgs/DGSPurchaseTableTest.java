/*
 *  Copyright © 2018-2010 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;


@Tag("slow")
public class DGSPurchaseTableTest extends EntityDbTableTest<DGSPurchase> {
    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/dgs-data.sql", "db/schema.sql");

    DGSPurchaseTable table;

    DGSTestData dtd;

    public DGSPurchaseTableTest() {
        super(DGSPurchase.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        table = new DGSPurchaseTable(extension.getDatabaseManager(), mock(Event.class));
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    public DGSPurchase valueToInsert() {
        return dtd.NEW_PURCHASE;
    }

    @Override
    public DerivedDbTable<DGSPurchase> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSPurchase> getAll() {
        return new ArrayList<>(List.of(dtd.PURCHASE_0, dtd.PURCHASE_1, dtd.PURCHASE_2, dtd.PURCHASE_3, dtd.PURCHASE_4, dtd.PURCHASE_5, dtd.PURCHASE_6, dtd.PURCHASE_7, dtd.PURCHASE_8, dtd.PURCHASE_9, dtd.PURCHASE_10, dtd.PURCHASE_11, dtd.PURCHASE_12, dtd.PURCHASE_13, dtd.PURCHASE_14, dtd.PURCHASE_15, dtd.PURCHASE_16, dtd.PURCHASE_17, dtd.PURCHASE_18));
    }

    @Override
    public Comparator<DGSPurchase> getDefaultComparator() {
        return Comparator.comparing(DGSPurchase::getTimestamp).reversed().thenComparing(DGSPurchase::getId);
    }

    @Override
    public List<DGSPurchase> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }
}
