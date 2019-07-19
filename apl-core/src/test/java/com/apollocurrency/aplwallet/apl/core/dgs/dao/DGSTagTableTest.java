/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSTag;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
@EnableWeld
public class DGSTagTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DGSTagTable.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();
    @Inject
    DGSTagTable table;


    DGSTestData dtd;


    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
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
}
