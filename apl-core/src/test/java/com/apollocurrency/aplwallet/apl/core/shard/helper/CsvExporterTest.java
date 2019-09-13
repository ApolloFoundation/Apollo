/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.PhasingOnly;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
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
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
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
import com.apollocurrency.aplwallet.apl.data.PrunableMessageTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

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
            "101,InCisA4/cPtdXY4No8eRnt1NM2gXbm8t,0,1",
            "102,uW1en2TlHFl1E3F2ke7urxiiaoZANPYs,1,1",
            "103,zKWh+CX5uRi+APNUBvcLEItmVrKZdVVY,2,1",
            "100,zG8XGTR3IJylgh0305HnCuZo3RwR3XmO,0,30");
    private List<String> transactionExportContent = List.of(
            "ID(-5|19|0),DEADLINE(5|5|0),RECIPIENT_ID(-5|19|0),TRANSACTION_INDEX(5|5|0),AMOUNT(-5|19|0),FEE(-5|19|0),FULL_HASH(-3|32|0),HEIGHT(4|10|0),BLOCK_ID(-5|19|0),SIGNATURE(-3|64|0),TIMESTAMP(4|10|0),TYPE(-6|3|0),SUBTYPE(-6|3|0),SENDER_ID(-5|19|0),SENDER_PUBLIC_KEY(-3|32|0),BLOCK_TIMESTAMP(4|10|0),REFERENCED_TRANSACTION_FULL_HASH(-3|32|0),PHASED(16|1|0),ATTACHMENT_BYTES(-3|2147483647|0),VERSION(-6|3|0),HAS_MESSAGE(16|1|0),HAS_ENCRYPTED_MESSAGE(16|1|0),HAS_PUBLIC_KEY_ANNOUNCEMENT(16|1|0),EC_BLOCK_HEIGHT(4|10|0),EC_BLOCK_ID(-5|19|0),HAS_ENCRYPTTOSELF_MESSAGE(16|1|0),HAS_PRUNABLE_MESSAGE(16|1|0),HAS_PRUNABLE_ENCRYPTED_MESSAGE(16|1|0),HAS_PRUNABLE_ATTACHMENT(16|1|0)",
            "3444674909301056677,1440,null,0,0,2500000000000,pSSXT5TxzS/MbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=,1000,-468651855371775066,N17xwFrlmifvJjNqWa/mkBTGi5v0Nk1bGy+k6+MCAgqGitNl818MqNPrrdxGns06fEnexeTS+tQfZyiXe3MzzA==,35073712,5,0,9211698109297098287,vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=,9200,ZAAAAAAAAADMbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=,FALSE,AQVmc2RmcwNRRVIFAGZzZGZzAa4VAAAAAAAAAAAAAAAAAACuFQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB,1,FALSE,FALSE,FALSE,14399,-5416619518547901377,FALSE,FALSE,FALSE,FALSE",
            "5373370077664349170,1440,457571885748888948,0,100000000000000000,100000000,8ovlxZ0Lkkq5bV6fZOUcWXUTcXaR7u6vGKJqhkA09iw=,1500,-7242168411665692630,iv06kdDjAR5QXgNTsfcInA1AFnL47V0N3CEH4LEwqgvdF/A7LXXu2PzGRc2oi1yCrBtiHBQqutmxu5XfUXqnDA==,35078473,0,0,9211698109297098287,vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=,13800,t8dFrkONVyEicKKwDj9w+11djg2jx5Ge3U0zaBduby0=,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE"
    );

    private List<String> blockTransactionExportContent = List.of(
            "ID(-5|19|0),DEADLINE(5|5|0),RECIPIENT_ID(-5|19|0),TRANSACTION_INDEX(5|5|0),AMOUNT(-5|19|0),FEE(-5|19|0),FULL_HASH(-3|32|0),HEIGHT(4|10|0),BLOCK_ID(-5|19|0),SIGNATURE(-3|64|0),TIMESTAMP(4|10|0),TYPE(-6|3|0),SUBTYPE(-6|3|0),SENDER_ID(-5|19|0),SENDER_PUBLIC_KEY(-3|32|0),BLOCK_TIMESTAMP(4|10|0),REFERENCED_TRANSACTION_FULL_HASH(-3|32|0),PHASED(16|1|0),ATTACHMENT_BYTES(-3|2147483647|0),VERSION(-6|3|0),HAS_MESSAGE(16|1|0),HAS_ENCRYPTED_MESSAGE(16|1|0),HAS_PUBLIC_KEY_ANNOUNCEMENT(16|1|0),EC_BLOCK_HEIGHT(4|10|0),EC_BLOCK_ID(-5|19|0),HAS_ENCRYPTTOSELF_MESSAGE(16|1|0),HAS_PRUNABLE_MESSAGE(16|1|0),HAS_PRUNABLE_ENCRYPTED_MESSAGE(16|1|0),HAS_PRUNABLE_ATTACHMENT(16|1|0)",
            "2083198303623116770,1440,-1017037002638468431,0,100000000000000000,100000000,4vcm5NEB6Rxu5zXJ2g1Vr3EAxFJjoKagkgwlWg9ltE8=,8000,6438949995368593549,0kgRvEvixwMRlv0iBjnxiFyOFcludnIUbIjC7qJdigzU6TuOIyTiUi46/xT6oe+BH8Q6lx/b23H3rAtWFOcGyw==,35078473,0,0,9211698109297098287,vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=,73600,9X/g0icw8EsBxaExtSCZNWyJmymt2wR22DXqLeXMVpE=,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE",
            "808614188720864902,1440,-5803127966835594607,1,100000000000000000,100000000,hj4MB1LGOAvnY1S9hhvgcFcR4O4rwLhNnw1xtaQnGvY=,8000,6438949995368593549,OEhMYSiycHqB6m8MnxlmPbzVQ1jmRNVs+iszY18tVw97kcQYIPjRkj4K/KXLDleFx2wv2FnjVMh2qWQKdYgqog==,35078473,0,0,9211698109297098287,vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=,73600,4vcm5NEB6Rxu5zXJ2g1Vr3EAxFJjoKagkgwlWg9ltE8=,FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE"
    );

    private List<String> blockExportContent = List.of(
            "ID(-5|19|0),VERSION(4|10|0),TIMESTAMP(4|10|0),PREVIOUS_BLOCK_ID(-5|19|0),TOTAL_AMOUNT(-5|19|0),TOTAL_FEE(-5|19|0),PAYLOAD_LENGTH(4|10|0),PREVIOUS_BLOCK_HASH(-3|32|0),CUMULATIVE_DIFFICULTY(-3|2147483647|0),BASE_TARGET(-5|19|0),NEXT_BLOCK_ID(-5|19|0),HEIGHT(4|10|0),GENERATION_SIGNATURE(-3|64|0),BLOCK_SIGNATURE(-3|64|0),PAYLOAD_HASH(-3|32|0),GENERATOR_ID(-5|19|0),TIMEOUT(4|10|0)",
            "6438949995368593549,4,73600,-5580266015477525080,0,200000000,207,qBVH25/pjrIk083BIPcwXTuCnxYr6zv3GXUODPSNvp0=,At+1GWz0FbA=,23058430050,7551185434952726924,8000,Wxv0Y/IC7A1KtCqWNJdu1Ht3xGLR3iXj/qPo6qit2PY=,mS6suKw7y7fb2/y2NzGK2rGQ1IQ7ANqJYf0272Bxjw9azKRmLP3PhEfMUR1eNqtMMhwYU4LzV38BBsK/ufgO5g==,i9+Y+8TPzwtm36pojOfvkGP4sXSO4jjCPoIJ8HHPzuc=,6415509874415488619,0"
    );

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class, DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
            JdbiHandleFactory.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class, PropertyBasedFileConfig.class,
            DataTagDao.class, KeyFactoryProducer.class, FeeCalculator.class,
            DGSGoodsTable.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DirProvider.class,
            AplAppStatus.class, PrunableMessageTable.class,
            PhasingPollResultTable.class,
            PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class,
            PhasingVoteTable.class, PhasingPollTable.class,
            AccountTable.class, AccountLedgerTable.class, DGSPurchaseTable.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            GenesisPublicKeyTable.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(AccountGuaranteedBalanceTable.class, AccountGuaranteedBalanceTable.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
//            .addBeans(MockBean.of(ftlEngine, FullTextSearchEngine.class)) // prod data test
//            .addBeans(MockBean.of(ftlService, FullTextSearchService.class)) // prod data test
            .addBeans(MockBean.of(keyStore, KeyStoreService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .build();
    @Inject
    ShardDao shardDao;
    @Inject
    AccountTable accountTable;
    @Inject
    PrunableMessageTable messageTable;
    @Inject
    TaggedDataDao taggedDataDao;

    @Inject
    PropertiesHolder propertiesHolder;
    @Inject
    DGSGoodsTable goodsTable;
    @Inject
    private Blockchain blockchain;
    @Inject
    DerivedTablesRegistry registry;

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
        Account.init(extension.getDatabaseManager(), propertiesHolder, null, null, blockchain, null, null, accountTable, null);
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
        csvExporter = new CsvExporterImpl(extension.getDatabaseManager(), dataExportPath);
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
    void testExportShardTable() throws Exception {
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
        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        List<String> lines = Files.readAllLines(shardExportedFile);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("BLOCK_TIMEOUTS"));
        assertTrue(lines.get(0).contains("BLOCK_TIMESTAMPS"));
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
    void testExportGoodsSortedByName() throws IOException {
        csvExporter.exportDerivedTableCustomSort(goodsTable, 542100, 2, Set.of("DB_ID", "LATEST", "ID", "SELLER_ID"), "name");
        List<String> allLines = Files.readAllLines(dataExportPath.resolve("goods.csv"));
        assertEquals(7, allLines.size());
        assertTrue(allLines.get(1).startsWith("\'1"));
        assertTrue(allLines.get(2).startsWith("\'Some product"));
        assertTrue(allLines.get(6).startsWith("\'Test product"));
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

    @Test
    void testExportPrunableMessageTable() throws URISyntaxException, IOException {
        doReturn(100).when(blockchainConfig).getMinPrunableLifetime();
        PrunableMessageTestData data = new PrunableMessageTestData();
        long exported = csvExporter.exportPrunableDerivedTable(messageTable, data.MESSAGE_6.getHeight() + 1, data.MESSAGE_11.getTransactionTimestamp(), 2);
        assertEquals(4, exported);
        List<String> allPrunableMessageData = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("prunable_message.csv").toURI()));
        List<String> expected = new ArrayList<>();
        expected.add(allPrunableMessageData.get(0));
        expected.addAll(allPrunableMessageData.subList(3, 7));
        List<String> actual = Files.readAllLines(dataExportPath.resolve("prunable_message.csv"));
        assertEquals(expected, actual);
    }

    @Test
    void testExportIgnoredTable() {
        DerivedTableInterface genesisTable = mock(DerivedTableInterface.class);
        doReturn("genesis_public_KEY").when(genesisTable).getName();
        doReturn(new MinMaxDbId(1, 2, 2, 2)).when(genesisTable).getMinMaxDbId(8000);
        long exported = csvExporter.exportDerivedTable(genesisTable, 8000, 2);
        assertEquals(-1, exported);
    }

    @Test
    void testExportShardTableIgnoringLastHashes() throws IOException, URISyntaxException {

        long exportedRows = csvExporter.exportShardTableIgnoringLastZipHashes(4, 1);

        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        assertEquals(2, exportedRows);
        long exportedFiles = Files.list(dataExportPath).count();
        assertEquals(1, exportedFiles);
        assertTrue(Files.exists(shardExportedFile));

        List<String> lines = Files.readAllLines(shardExportedFile);
        assertEquals(3, lines.size());
        List<String> expectedRows = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("shard-last-hashes-null.csv").toURI()));
        assertEquals(expectedRows, lines);
    }

    @Test
    void testExportShardTableIgnoringLastHashesWhenNoShardsInDb() throws IOException {
        shardDao.hardDeleteAllShards();

        long exportedRows = csvExporter.exportShardTableIgnoringLastZipHashes(Integer.MAX_VALUE, 1);
        assertEquals(0, exportedRows);

        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        long exportedFiles = Files.list(dataExportPath).count();
        assertEquals(0, exportedFiles);
        assertFalse(Files.exists(shardExportedFile));
    }
    @Test
    void testExportShardTableIgnoringLastHashesWhenOnlyOneShardExists() throws IOException, URISyntaxException {
        long exportedRows = csvExporter.exportShardTableIgnoringLastZipHashes(2, 1);
        assertEquals(1, exportedRows);

        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        long exportedFiles = Files.list(dataExportPath).count();
        assertEquals(1, exportedFiles);
        assertTrue(Files.exists(shardExportedFile));

        List<String> lines = Files.readAllLines(shardExportedFile);
        assertEquals(2, lines.size());
        List<String> expectedRows = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("shard-last-hashes-null.csv").toURI())).subList(0, 2);
        assertEquals(expectedRows, lines);
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