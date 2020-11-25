/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.CurrencyTransferTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@QuarkusTest
class CurrencyTransferTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/currency_transfer-data.sql", "db/schema.sql");

    @Inject
    CurrencyTransferTable table;
    CurrencyTransferTestData td;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, CurrencyTransferTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();*/

    @BeforeEach
    void setUp() {
        td = new CurrencyTransferTestData();
    }

    @Test
    void testLoad() {
        CurrencyTransfer transfer = table.get(table.getDbKeyFactory().newKey(td.TRANSFER_0));
        assertNotNull(transfer);
        assertEquals(td.TRANSFER_0, transfer);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        dbExtension.cleanAndPopulateDb();

        CurrencyTransfer transfer = table.get(table.getDbKeyFactory().newKey(td.TRANSFER_NEW));
        assertNull(transfer);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        CurrencyTransfer previous = table.get(table.getDbKeyFactory().newKey(td.TRANSFER_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.TRANSFER_NEW));
        CurrencyTransfer actual = table.get(table.getDbKeyFactory().newKey(td.TRANSFER_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.TRANSFER_NEW.getCurrencyId(), actual.getCurrencyId());
        assertEquals(td.TRANSFER_NEW.getRecipientId(), actual.getRecipientId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        CurrencyTransfer previous = table.get(table.getDbKeyFactory().newKey(td.TRANSFER_1));
        assertNotNull(previous);

        assertThrows(RuntimeException.class, () -> // not permitted by DB constraints
            DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous))
        );
    }

    @Test
    void getAccountCurrencyTransfers() {
        DbIterator<CurrencyTransfer> dbResult = table.getAccountCurrencyTransfers(-7396849795322372927L, 0, 10);
        List<CurrencyTransfer> result = CollectionUtil.toList(dbResult);
        assertEquals(List.of(td.TRANSFER_2, td.TRANSFER_0), result);
    }

    @Test
    void getAccountCurrencyTransfers_2() {
        DbIterator<CurrencyTransfer> dbResult = table.getAccountCurrencyTransfers(
            -208393164898941117L, -5453448652141572559L, 0, 10);
        List<CurrencyTransfer> result = CollectionUtil.toList(dbResult);
        assertEquals(List.of(td.TRANSFER_1), result);
    }

}