/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Tag("slow")
public class DGSGoodsTableTest extends EntityDbTableTest<DGSGoods> {

    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);
    DGSGoodsTable table = new DGSGoodsTable(getDatabaseManager(), fullTextOperationDataEvent);

    DGSTestData dtd = new DGSTestData();

    public DGSGoodsTableTest() {
        super(DGSGoods.class);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
    }

    @Override
    public DerivedDbTable<DGSGoods> getDerivedDbTable() {
        return table;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return null;
    }

    @Override
    protected List<DGSGoods> getAll() {
        return new ArrayList<>(List.of(dtd.GOODS_0, dtd.GOODS_1, dtd.GOODS_2, dtd.GOODS_3, dtd.GOODS_4, dtd.GOODS_5, dtd.GOODS_6, dtd.GOODS_7, dtd.GOODS_8, dtd.GOODS_9, dtd.GOODS_10, dtd.GOODS_11, dtd.GOODS_12, dtd.GOODS_13));
    }

    @Override
    public DGSGoods valueToInsert() {
        return dtd.NEW_GOODS;
    }

    @Override
    public Comparator<DGSGoods> getDefaultComparator() {
        return Comparator.comparing(DGSGoods::getTimestamp).reversed().thenComparing(DGSGoods::getId);
    }

    @Override
    public List<DGSGoods> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
   }

    @Test
    void testRollback() {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> table.rollback(dtd.GOODS_3.getHeight()));
        verify(fullTextOperationDataEvent, times(10)).select(new AnnotationLiteral<TrimEvent>() {});
    }

}
