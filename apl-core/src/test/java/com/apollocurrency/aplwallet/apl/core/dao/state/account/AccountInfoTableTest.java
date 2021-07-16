/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@EnableWeld
class AccountInfoTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, Map.of("account_info", List.of("name", "description")));
    @Inject
    AccountInfoTable table;
    AccountTestData testData = new AccountTestData();
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class,
        FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class,
        AccountInfoTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(dbExtension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(dbExtension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
        .build();

    @Tag("skip-fts-init")
    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        dbExtension.cleanAndPopulateDb();
        AccountInfo previous = table.get(table.getDbKeyFactory().newKey(testData.newInfo));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newInfo));
        AccountInfo actual = table.get(table.getDbKeyFactory().newKey(testData.newInfo));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newInfo.getAccountId(), actual.getAccountId());
        assertEquals(testData.newInfo.getName(), actual.getName());
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        dbExtension.cleanAndPopulateDb();
        AccountInfo previous = table.get(table.getDbKeyFactory().newKey(testData.ACC_INFO_0));
        assertNotNull(previous);
        previous.setName("Ping-Pong " + previous.getName());

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountInfo actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertTrue(actual.getName().startsWith("Ping-Pong "));
        assertEquals(previous.getDescription(), actual.getDescription());
    }

}