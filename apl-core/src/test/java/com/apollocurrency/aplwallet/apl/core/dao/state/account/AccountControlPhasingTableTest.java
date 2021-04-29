/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.AccountControlPhasingTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class AccountControlPhasingTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbConfig(), "db/data.sql", "db/schema.sql");

    @Inject
    AccountControlPhasingTable table;
    AccountControlPhasingTestData td;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountControlPhasingTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new AccountControlPhasingTestData();
    }

    @Test
    void testLoad() {
        AccountControlPhasing phasing = table.get(table.getDbKeyFactory().newKey(td.AC_CONT_PHAS_0));
        assertNotNull(phasing);
        assertEquals(td.AC_CONT_PHAS_0, phasing);
    }

    @Test
    void load_returnNull_ifNotExist() {
        AccountControlPhasing phasing = table.get(table.getDbKeyFactory().newKey(td.NEW_AC_CONT_PHAS));
        assertNull(phasing);
    }

    @Test
    void testSave() {
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.NEW_AC_CONT_PHAS));
        AccountControlPhasing phasing = table.get(table.getDbKeyFactory().newKey(td.NEW_AC_CONT_PHAS));
        assertNotNull(phasing);
        assertTrue(phasing.getDbId() != 0);
        assertEquals(td.NEW_AC_CONT_PHAS.getAccountId(), phasing.getAccountId());
        assertEquals(td.NEW_AC_CONT_PHAS.getHeight(), phasing.getHeight());
    }

    @Test
    void testSave_update_existing_entity() {
        AccountControlPhasing previous = table.get(table.getDbKeyFactory().newKey(td.AC_CONT_PHAS_1));
        assertNotNull(previous);
        long value = 100L;
        previous.setMaxFees(value);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountControlPhasing actual = table.get(table.getDbKeyFactory().newKey(td.AC_CONT_PHAS_1));

        assertNotNull(actual);
        assertEquals(value, actual.getMaxFees());
    }

    @Test
    void test_delete_entity() {
        AccountControlPhasing found = table.get(table.getDbKeyFactory().newKey(td.AC_CONT_PHAS_3));
        assertNotNull(found);

        DbUtils.inTransaction(dbExtension, (con) -> {
            boolean result = table.deleteAtHeight(found, td.AC_CONT_PHAS_3.getHeight());
            assertTrue(result);
        });
        AccountControlPhasing actual = table.get(table.getDbKeyFactory().newKey(td.AC_CONT_PHAS_3));

        assertNull(actual);
    }


}