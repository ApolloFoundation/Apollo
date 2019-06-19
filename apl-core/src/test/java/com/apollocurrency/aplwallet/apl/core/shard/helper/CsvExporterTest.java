/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.VaultKeyStoreServiceImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyBasedFileConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.config.WalletClientProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOfferMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.IndexTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.slf4j.Logger;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class CsvExporterTest {
    private static final Logger log = getLogger(CsvExporterTest.class);

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("csvExporterDb").toAbsolutePath().toString()));
    //    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("apl-blockchain").toAbsolutePath().toString())); // prod data test
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private NtpTime time = mock(NtpTime.class);
//    private LuceneFullTextSearchEngine ftlEngine = new LuceneFullTextSearchEngine(time, createPath("indexDirPath")); // prod data test
//    private FullTextSearchService ftlService = new FullTextSearchServiceImpl(extension.getDatabaseManager(),
//        ftlEngine, Set.of("tagged_data", "currency"), "PUBLIC"); // prod data test
    private KeyStoreService keyStore = new VaultKeyStoreServiceImpl(temporaryFolderExtension.newFolder("keystorePath").toPath(), time);
//    private KeyStoreService keyStore = new VaultKeyStoreServiceImpl(createPath("keystorePath"), time); // prod data test
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private List<String> blockIndexExportContent = List.of("BLOCK_ID(-5|19|0),BLOCK_HEIGHT(4|10|0)", "1,1", "2,2", "3,30");
    private List<String> transactionIndexExportContent = List.of(
            "TRANSACTION_ID(-5|19|0),PARTIAL_TRANSACTION_HASH(-3|2147483647|0),TRANSACTION_INDEX(5|5|0),HEIGHT(4|10|0)",
            "101,MjI3MGEyYjAwZTNmNzBmYjVkNWQ4ZTBkYTNjNzkxOWVkZDRkMzM2ODE3NmU2ZjJk,0,1",
            "102,Yjk2ZDVlOWY2NGU1MWM1OTc1MTM3MTc2OTFlZWVlYWYxOGEyNmE4NjQwMzRmNjJj,1,1",
            "103,Y2NhNWExZjgyNWY5YjkxOGJlMDBmMzU0MDZmNzBiMTA4YjY2NTZiMjk5NzU1NTU4,2,1",
            "100,Y2M2ZjE3MTkzNDc3MjA5Y2E1ODIxZDM3ZDM5MWU3MGFlNjY4ZGQxYzExZGQ3OThl,0,30");
    private List<String> transactionExportContent = List.of(
            "ID(-5|19|0),DEADLINE(5|5|0),RECIPIENT_ID(-5|19|0),TRANSACTION_INDEX(5|5|0),AMOUNT(-5|19|0),FEE(-5|19|0),FULL_HASH(-3|32|0),HEIGHT(4|10|0),BLOCK_ID(-5|19|0),SIGNATURE(-3|64|0),TIMESTAMP(4|10|0),TYPE(-6|3|0),SUBTYPE(-6|3|0),SENDER_ID(-5|19|0),SENDER_PUBLIC_KEY(-3|32|0),BLOCK_TIMESTAMP(4|10|0),REFERENCED_TRANSACTION_FULL_HASH(-3|32|0),PHASED(16|1|0),ATTACHMENT_BYTES(-3|2147483647|0),VERSION(-6|3|0),HAS_MESSAGE(16|1|0),HAS_ENCRYPTED_MESSAGE(16|1|0),HAS_PUBLIC_KEY_ANNOUNCEMENT(16|1|0),EC_BLOCK_HEIGHT(4|10|0),EC_BLOCK_ID(-5|19|0),HAS_ENCRYPTTOSELF_MESSAGE(16|1|0),HAS_PRUNABLE_MESSAGE(16|1|0),HAS_PRUNABLE_ENCRYPTED_MESSAGE(16|1|0),HAS_PRUNABLE_ATTACHMENT(16|1|0)",
            "3444674909301056677,1440,null,0,0,2500000000000,YTUyNDk3NGY5NGYxY2QyZmNjNmYxNzE5MzQ3NzIwOWNhNTgyMWQzN2QzOTFlNzBhZTY2OGRkMWMxMWRkNzk4ZQ==,1000,-468651855371775066,Mzc1ZWYxYzA1YWU1OWEyN2VmMjYzMzZhNTlhZmU2OTAxNGM2OGI5YmY0MzY0ZDViMWIyZmE0ZWJlMzAyMDIwYTg2OGFkMzY1ZjM1ZjBjYThkM2ViYWRkYzQ2OWVjZDNhN2M0OWRlYzVlNGQyZmFkNDFmNjcyODk3N2I3MzMzY2M=,35073712,5,0,9211698109297098287,YmYwY2VkMDQ3MmQ4YmEzZGY5ZTIxODA4ZTk4ZTYxYjM0NDA0YWFkNzM3ZTJiYWUxNzc4Y2ViYzY5OGI0MGYzNw==,9200,NjQwMDAwMDAwMDAwMDAwMGNjNmYxNzE5MzQ3NzIwOWNhNTgyMWQzN2QzOTFlNzBhZTY2OGRkMWMxMWRkNzk4ZQ==,FALSE,MDEwNTY2NzM2NDY2NzMwMzUxNDU1MjA1MDA2NjczNjQ2NjczMDFhZTE1MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMGFlMTUwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAx,1,FALSE,FALSE,FALSE,14399,-5416619518547901377,FALSE,FALSE,FALSE,FALSE",
            "5373370077664349170,1440,457571885748888948,0,100000000000000000,100000000,ZjI4YmU1YzU5ZDBiOTI0YWI5NmQ1ZTlmNjRlNTFjNTk3NTEzNzE3NjkxZWVlZWFmMThhMjZhODY0MDM0ZjYyYw==,1500,-7242168411665692630,OGFmZDNhOTFkMGUzMDExZTUwNWUwMzUzYjFmNzA4OWMwZDQwMTY3MmY4ZWQ1ZDBkZGMyMTA3ZTBiMTMwYWEwYmRkMTdmMDNiMmQ3NWVlZDhmY2M2NDVjZGE4OGI1YzgyYWMxYjYyMWMxNDJhYmFkOWIxYmI5NWRmNTE3YWE3MGM=,35078473,0,0,9211698109297098287,YmYwY2VkMDQ3MmQ4YmEzZGY5ZTIxODA4ZTk4ZTYxYjM0NDA0YWFkNzM3ZTJiYWUxNzc4Y2ViYzY5OGI0MGYzNw==,13800,YjdjNzQ1YWU0MzhkNTcyMTIyNzBhMmIwMGUzZjcwZmI1ZDVkOGUwZGEzYzc5MTllZGQ0ZDMzNjgxNzZlNmYyZA==,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE"
    );

    private List<String> blockTransactionExportContent = List.of(
            "ID(-5|19|0),DEADLINE(5|5|0),RECIPIENT_ID(-5|19|0),TRANSACTION_INDEX(5|5|0),AMOUNT(-5|19|0),FEE(-5|19|0),FULL_HASH(-3|32|0),HEIGHT(4|10|0),BLOCK_ID(-5|19|0),SIGNATURE(-3|64|0),TIMESTAMP(4|10|0),TYPE(-6|3|0),SUBTYPE(-6|3|0),SENDER_ID(-5|19|0),SENDER_PUBLIC_KEY(-3|32|0),BLOCK_TIMESTAMP(4|10|0),REFERENCED_TRANSACTION_FULL_HASH(-3|32|0),PHASED(16|1|0),ATTACHMENT_BYTES(-3|2147483647|0),VERSION(-6|3|0),HAS_MESSAGE(16|1|0),HAS_ENCRYPTED_MESSAGE(16|1|0),HAS_PUBLIC_KEY_ANNOUNCEMENT(16|1|0),EC_BLOCK_HEIGHT(4|10|0),EC_BLOCK_ID(-5|19|0),HAS_ENCRYPTTOSELF_MESSAGE(16|1|0),HAS_PRUNABLE_MESSAGE(16|1|0),HAS_PRUNABLE_ENCRYPTED_MESSAGE(16|1|0),HAS_PRUNABLE_ATTACHMENT(16|1|0)",
            "2083198303623116770,1440,-1017037002638468431,0,100000000000000000,100000000,ZTJmNzI2ZTRkMTAxZTkxYzZlZTczNWM5ZGEwZDU1YWY3MTAwYzQ1MjYzYTBhNmEwOTIwYzI1NWEwZjY1YjQ0Zg==,8000,6438949995368593549,ZDI0ODExYmM0YmUyYzcwMzExOTZmZDIyMDYzOWYxODg1YzhlMTVjOTZlNzY3MjE0NmM4OGMyZWVhMjVkOGEwY2Q0ZTkzYjhlMjMyNGUyNTIyZTNhZmYxNGZhYTFlZjgxMWZjNDNhOTcxZmRiZGI3MWY3YWMwYjU2MTRlNzA2Y2I=,35078473,0,0,9211698109297098287,YmYwY2VkMDQ3MmQ4YmEzZGY5ZTIxODA4ZTk4ZTYxYjM0NDA0YWFkNzM3ZTJiYWUxNzc4Y2ViYzY5OGI0MGYzNw==,73600,ZjU3ZmUwZDIyNzMwZjA0YjAxYzVhMTMxYjUyMDk5MzU2Yzg5OWIyOWFkZGIwNDc2ZDgzNWVhMmRlNWNjNTY5MQ==,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE",
            "808614188720864902,1440,-5803127966835594607,1,100000000000000000,100000000,ODYzZTBjMDc1MmM2MzgwYmU3NjM1NGJkODYxYmUwNzA1NzExZTBlZTJiYzBiODRkOWYwZDcxYjVhNDI3MWFmNg==,8000,6438949995368593549,Mzg0ODRjNjEyOGIyNzA3YTgxZWE2ZjBjOWYxOTY2M2RiY2Q1NDM1OGU2NDRkNTZjZmEyYjMzNjM1ZjJkNTcwZjdiOTFjNDE4MjBmOGQxOTIzZTBhZmNhNWNiMGU1Nzg1Yzc2YzJmZDg1OWUzNTRjODc2YTk2NDBhNzU4ODJhYTI=,35078473,0,0,9211698109297098287,YmYwY2VkMDQ3MmQ4YmEzZGY5ZTIxODA4ZTk4ZTYxYjM0NDA0YWFkNzM3ZTJiYWUxNzc4Y2ViYzY5OGI0MGYzNw==,73600,ZTJmNzI2ZTRkMTAxZTkxYzZlZTczNWM5ZGEwZDU1YWY3MTAwYzQ1MjYzYTBhNmEwOTIwYzI1NWEwZjY1YjQ0Zg==,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE"
    );

    private List<String> blockExportContent = List.of(
            "ID(-5|19|0),VERSION(4|10|0),TIMESTAMP(4|10|0),PREVIOUS_BLOCK_ID(-5|19|0),TOTAL_AMOUNT(-5|19|0),TOTAL_FEE(-5|19|0),PAYLOAD_LENGTH(4|10|0),PREVIOUS_BLOCK_HASH(-3|32|0),CUMULATIVE_DIFFICULTY(-3|2147483647|0),BASE_TARGET(-5|19|0),NEXT_BLOCK_ID(-5|19|0),HEIGHT(4|10|0),GENERATION_SIGNATURE(-3|64|0),BLOCK_SIGNATURE(-3|64|0),PAYLOAD_HASH(-3|32|0),GENERATOR_ID(-5|19|0),TIMEOUT(4|10|0)",
            "6438949995368593549,4,73600,-5580266015477525080,0,200000000,207,YTgxNTQ3ZGI5ZmU5OGViMjI0ZDNjZGMxMjBmNzMwNWQzYjgyOWYxNjJiZWIzYmY3MTk3NTBlMGNmNDhkYmU5ZA==,MDJkZmI1MTk2Y2Y0MTViMA==,23058430050,7551185434952726924,8000,NWIxYmY0NjNmMjAyZWMwZDRhYjQyYTk2MzQ5NzZlZDQ3Yjc3YzQ2MmQxZGUyNWUzZmVhM2U4ZWFhOGFkZDhmNg==,OTkyZWFjYjhhYzNiY2JiN2RiZGJmY2I2MzczMThhZGFiMTkwZDQ4NDNiMDBkYTg5NjFmZDM2ZWY2MDcxOGYwZjVhY2NhNDY2MmNmZGNmODQ0N2NjNTExZDVlMzZhYjRjMzIxYzE4NTM4MmYzNTc3ZjAxMDZjMmJmYjlmODBlZTY=,OGJkZjk4ZmJjNGNmY2YwYjY2ZGZhYTY4OGNlN2VmOTA2M2Y4YjE3NDhlZTIzOGMyM2U4MjA5ZjA3MWNmY2VlNw==,6415509874415488619,0"
    );

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class, DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
            TrimService.class, ShardDaoJdbcImpl.class,
            JdbiHandleFactory.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class, DexService.class, DexOfferTable.class, EthereumWalletService.class,
            DexOfferMapper.class, WalletClientProducer.class, PropertyBasedFileConfig.class,
            DataTagDao.class, KeyFactoryProducer.class, FeeCalculator.class,
            DGSGoodsTable.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DirProvider.class,
            AccountTable.class, AccountLedgerTable.class, DGSPurchaseTable.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(AccountGuaranteedBalanceTable.class, AccountGuaranteedBalanceTable.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
//            .addBeans(MockBean.of(ftlEngine, FullTextSearchEngine.class)) // prod data test
//            .addBeans(MockBean.of(ftlService, FullTextSearchService.class)) // prod data test
            .addBeans(MockBean.of(keyStore, KeyStoreService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .build();

    @Inject
    AccountTable accountTable;
    @Inject
    PropertiesHolder propertiesHolder;
    @Inject
    DGSGoodsTable goodsTable;
    @Inject
    private Blockchain blockchain;
    @Inject
    DerivedTablesRegistry registry;
    @Inject
    ShardDaoJdbc shardDaoJdbc;

    CsvExporter csvExporter;

    private Path dataExportPath;

    public CsvExporterTest() throws Exception {
    }

    private Path createPath(String fileName) {
        try {
            Path folder = temporaryFolderExtension.newFolder().toPath().resolve(fileName);
            Files.createDirectories(folder);
            return folder;
            //            return Path.of("/Apollo/apl-core/unit-test-perm" + (fileName !=null ? ("/" + fileName) : "")); // prod data test
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        // init several derived tables
        AccountCurrencyTable.getInstance().init();
        Account.init(extension.getDatabaseManager(), propertiesHolder, null, null, blockchain, null, null, accountTable);
        AccountInfoTable.getInstance().init();
        Alias.init();
        PhasingOnly.get(7995581942006468815L); // works OK!
        PhasingOnly.get(2728325718715804811L); // check 1
        PhasingOnly.get(-8446384352342482748L); // check 2
        PhasingOnly.get(-4013722529644937202L); // works OK!
        AccountAssetTable.getInstance().init();
        PublicKeyTable publicKeyTable = new PublicKeyTable(blockchain);
        publicKeyTable.init();
        dataExportPath = createPath("csvExportDir");
        csvExporter = new CsvExporterImpl(extension.getDatabaseManager(), dataExportPath, shardDaoJdbc);
    }

    @Test
    void exportDerivedTables() throws Exception {
        Collection<DerivedTableInterface> result = registry.getDerivedTables(); // extract all derived tables
        int targetHeight = 8000;
//        int targetHeight = 2_000_000; // prod data test
        int batchLimit = 1; // used for pagination and partial commit
        int[] tablesWithDataCount = new int[1]; // some table doesn't have exported data

        long start = System.currentTimeMillis();
        result.forEach(item -> {
            long start2 = System.currentTimeMillis();
            long exportedRows = 0;
            exportedRows = csvExporter.exportDerivedTable(item, targetHeight, batchLimit);
            if (exportedRows > 0) {
                tablesWithDataCount[0] = tablesWithDataCount[0] + 1;
            }
            log.debug("Processed Table = {}, exported = '{}' rows in {} secs", item, exportedRows, (System.currentTimeMillis() - start2) / 1000);
        });
        log.debug("Total Tables = [{}] in {} sec", result.size(), (System.currentTimeMillis() - start) / 1000);
        String[] extensions = new String[]{"csv"};
        Collection filesInFolder = FileUtils.listFiles(dataExportPath.toFile(), extensions, false);
        assertNotNull(filesInFolder);
        assertTrue(filesInFolder.size() > 0);
        assertEquals(tablesWithDataCount[0], filesInFolder.size(), "wrong number processed/exported tables and real CSV files in folder");
        log.debug("Exported Tables with data = [{}]", filesInFolder.size());
        log.debug("Processed list = '{}' in {} sec", result, (System.currentTimeMillis() - start) / 1000);

        // check if csv content is not empty
        for (Object next : filesInFolder) {
            String fileName = ((File) next).getName();
            log.trace("File in folder = {}", fileName);
            int readCount = importCsvAndCheckContent(fileName, dataExportPath);
            assertTrue(readCount > 0);
        }
    }

    @Test
    void exportShardTable() throws Exception {
        String tableName = "shard";
        int targetHeight = 3;
        int batchLimit = 1; // used for pagination and partial commit

        long exportedRows = csvExporter.exportShardTable(targetHeight, batchLimit);
        log.debug("Processed Tables = {}, exported = '{}' rows", tableName, exportedRows);

        String[] extensions = new String[]{"csv"};
        Collection filesInFolder = FileUtils.listFiles(dataExportPath.toFile(), extensions, false);
        assertNotNull(filesInFolder);
        assertEquals(1, filesInFolder.size());
        ((File) filesInFolder.iterator().next()).getName().equalsIgnoreCase(tableName + CsvAbstractBase.CSV_FILE_EXTENSION);

        // check if csv content is not empty
        Iterator iterator = filesInFolder.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            String fileName = ((File)next).getName();
            log.trace("File in folder = {}", fileName);
            int readCount = importCsvAndCheckContent(fileName, dataExportPath);
            assertTrue(readCount > 0);
        }

        log.debug("Processed Table = [{}]", filesInFolder.size());
    }

    @Test
    void testExportBlockIndex() throws IOException {
        long exported = csvExporter.exportBlockIndex(IndexTestData.BLOCK_INDEX_0.getBlockHeight(), 2);
        assertEquals(2, exported);
        List<String> blockIndexCsv = Files.readAllLines(dataExportPath.resolve("block_index.csv"));
        assertEquals(blockIndexExportContent.subList(0, 3), blockIndexCsv);
    }

    @Test
    void testExportTransactionIndex() throws IOException {
        long exported = csvExporter.exportTransactionIndex(IndexTestData.BLOCK_INDEX_0.getBlockHeight(), 2);
        assertEquals(3, exported);
        List<String> transactionIndexCsv = Files.readAllLines(dataExportPath.resolve("transaction_shard_index.csv"));
        assertEquals(transactionIndexExportContent.subList(0, 4), transactionIndexCsv);
    }


    @Test
    void testExportFullTransactionIndex() throws IOException {
        long exported = csvExporter.exportTransactionIndex(IndexTestData.BLOCK_INDEX_0.getBlockHeight() + 1, 1);
        assertEquals(4, exported);
        List<String> transactionIndexCsv = Files.readAllLines(dataExportPath.resolve("transaction_shard_index.csv"));
        assertEquals(transactionIndexExportContent, transactionIndexCsv);
    }

    @Test
    void testExportAllIndexes() throws IOException {
        long exported = csvExporter.exportBlockIndex(IndexTestData.BLOCK_INDEX_0.getBlockHeight() + 1, 2);
        assertEquals(3, exported);
        List<String> blockIndexCsv = Files.readAllLines(dataExportPath.resolve("block_index.csv"));
        assertEquals(blockIndexExportContent, blockIndexCsv);
    }

    @Test
    void testExportTransactions() throws IOException {
        TransactionTestData td = new TransactionTestData();
        long exported = csvExporter.exportTransactions(List.of(td.DB_ID_2, td.DB_ID_0));
        assertEquals(2, exported);
        List<String> transactionCsv = Files.readAllLines(dataExportPath.resolve("transaction.csv"));
        assertEquals(transactionExportContent, transactionCsv);
    }

    @Test
    void testExportGoodsTable() throws URISyntaxException, IOException {
        long exported = csvExporter.exportDerivedTable(goodsTable, 542100, 2);
        assertEquals(6, exported);
        List<String> goodsCsv = Files.readAllLines(dataExportPath.resolve("goods.csv"));
        List<String> expectedGoodsCsv = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("goods.csv").toURI()));
        assertEquals(expectedGoodsCsv.subList(0, 7), goodsCsv);
    }

    @Test
    void testExportBlock() throws IOException {
        BlockTestData td = new BlockTestData();
        long exported = csvExporter.exportBlock(td.BLOCK_7.getHeight());
        assertEquals(3, exported);
        List<String> exportedBlockTransactions = Files.readAllLines(dataExportPath.resolve("transaction.csv"));
        assertEquals(blockTransactionExportContent, exportedBlockTransactions);

        List<String> exportedBlock = Files.readAllLines(dataExportPath.resolve("block.csv"));
        assertEquals(blockExportContent, exportedBlock);
    }

    @Test
    void testExportBlockWhichNotExist() {
        assertThrows(IllegalStateException.class, ()-> csvExporter.exportBlock(Integer.MAX_VALUE));
    }

    @Test
    void testExportBlockWithoutTransactions() throws IOException {
        BlockTestData td = new BlockTestData();
        long exported = csvExporter.exportBlock(td.GENESIS_BLOCK.getHeight());
        assertEquals(1, exported);
        assertTrue(Files.exists(dataExportPath.resolve("block.csv")));
        List<String> header = transactionExportContent.subList(0, 1);
        assertEquals(header, Files.readAllLines(dataExportPath.resolve("transaction.csv")));

    }

    private int importCsvAndCheckContent(String itemName, Path dataExportDir) throws Exception {
        int readRowsFromFile = 0;

        // open CSV Reader and read data
        try (CsvReader csvReader = new CsvReaderImpl(dataExportDir);
             ResultSet rs = csvReader.read(itemName, null, null) ) {
            csvReader.setOptions("fieldDelimiter="); // do not put ""

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            int columnsCount = meta.getColumnCount(); // columns count is main
            StringBuilder columnNames = new StringBuilder(200);

            for (int i = 0; i < columnsCount; i++) {
                columnNames.append(meta.getColumnLabel(i + 1)).append(",");
            }
            log.debug("'{}' column HEADERS = {}", itemName, columnNames.toString()); // read headers
            assertTrue(columnNames.toString().length() > 0, "headers row is empty for '" + itemName + "'");

            while (rs.next()) {
                for (int j = 0; j < columnsCount; j++) {
                    Object object = rs.getObject(j + 1); // can be NULL sometimes
                    log.trace("Row column [{}] value is {}", meta.getColumnLabel(j + 1) , object);
                }
                readRowsFromFile++;
            }
        }
        return readRowsFromFile;
    }
}