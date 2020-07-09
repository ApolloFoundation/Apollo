/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Tag("slow")
@EnableWeld
public class ReferencedTransactionServiceTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("targetDb").toAbsolutePath().toString()));
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        TransactionImpl.class, BlockchainImpl.class, DaoConfig.class,
        ReferencedTransactionDaoImpl.class,
        GlobalSync.class,
        FullTextConfigImpl.class,
        GlobalSyncImpl.class,
        DerivedDbTablesRegistryImpl.class,
        TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class, ReferencedTransactionService.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .build();
    HeightConfig config = Mockito.mock(HeightConfig.class);
    @Inject
    ReferencedTransactionService service;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
    }

    @Test
    void testHasAllReferencedTransaction() {
        TransactionTestData td = new TransactionTestData();
        doReturn(1000).when(config).getReferencedTransactionHeightSpan();
        boolean hasAll = service.hasAllReferencedTransactions(td.TRANSACTION_0, 1000);

        assertTrue(hasAll);
    }

    @Test
    void testHasNotAllReferencedTransactionsWhenHeightIsNotEnough() {
        TransactionTestData td = new TransactionTestData();
        doReturn(1000).when(config).getReferencedTransactionHeightSpan();
        boolean hasAll = service.hasAllReferencedTransactions(td.TRANSACTION_5, td.TRANSACTION_5.getHeight());

        assertFalse(hasAll);
    }

    @Test
    void testHasNotAllReferencedTransactionsWhenTransactionHeightIsLessThanHeightOfReferencedTransactions() {
        TransactionTestData td = new TransactionTestData();
        doReturn(20_000).when(config).getReferencedTransactionHeightSpan();
        boolean hasAll = service.hasAllReferencedTransactions(td.TRANSACTION_11, td.TRANSACTION_11.getHeight());

        assertFalse(hasAll);
    }

    @Test
    void testHasNotAllReferencedTransactionWhenMaximumNumberOfReferencedTransactionsReached() {
        doReturn(20_000).when(config).getReferencedTransactionHeightSpan();
        TransactionTestData td = new TransactionTestData();
        boolean hasAll = service.hasAllReferencedTransactions(td.TRANSACTION_9, td.TRANSACTION_9.getHeight());

        assertFalse(hasAll);

        hasAll = service.hasAllReferencedTransactions(td.TRANSACTION_8, td.TRANSACTION_8.getHeight());

        assertFalse(hasAll);
    }


}
