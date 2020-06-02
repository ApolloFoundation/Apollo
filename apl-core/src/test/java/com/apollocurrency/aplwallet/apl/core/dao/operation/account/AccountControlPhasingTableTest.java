/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.operation.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.dao.operation.account.AccountControlPhasingTable;
import com.apollocurrency.aplwallet.apl.core.entity.operation.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.data.AccountControlPhasingData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@EnableWeld
class AccountControlPhasingTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/data.sql", "db/schema.sql");

    @Inject
    AccountControlPhasingTable table;
    AccountControlPhasingData td;

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

    private static Path createPath(String fileName) {
        try {
            return Files.createTempDirectory(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    void setUp() {
        td = new AccountControlPhasingData();
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