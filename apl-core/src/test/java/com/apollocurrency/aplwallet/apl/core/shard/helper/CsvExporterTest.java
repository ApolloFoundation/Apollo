/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyBasedFileConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountControlPhasingTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.TableRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.IndexTestData;
import com.apollocurrency.aplwallet.apl.data.PrunableMessageTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ServiceModeDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class CsvExporterTest extends DbContainerBaseTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbFileProperties(createPath("csvExporterDb").toAbsolutePath().toString()));
    @Inject
    private ShardDao shardDao;
    @Inject
    private PrunableMessageTable messageTable;
    @Inject
    private DGSGoodsTable goodsTable;
    @Inject
    private DerivedTablesRegistry registry;
    @Inject
    TableRegistryInitializer initializer;
    @Inject
    private DexOrderTable dexOrderTable;
    private CsvExporter csvExporter;
    @Inject
    private CsvEscaper translator;
    @Inject
    private Event<DeleteOnTrimData> deleteOnTrimDataEvent;
    @Inject
    private DerivedTablesRegistry derivedTablesRegistry;
    @Inject
    private FullTextConfig fullTextConfig;
    @Inject
    AccountCurrencyTable accountCurrencyTable;
    @Inject
    AccountInfoTable accountInfoTable;
    @Inject
    AccountControlPhasingTable accountControlPhasingTable;

    @Inject
    AccountAssetTable accountAssetTable;
    @Inject
    @Named("publicKeyTable")
    EntityDbTableInterface<PublicKey> publicKeyTable;
    @Inject
    DexContractTable dexContractTable;





    private Path dataExportPath;
    private BlockchainConfig blockchainConfig = mockBlockchainConfig();
    private PropertiesHolder propertiesHolder = mockPropertiesHolder();


    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private PeersService peersService = mock(PeersService.class);
    private GeneratorService generatorService = mock(GeneratorService.class);
    private TransactionTestData td = new TransactionTestData();
    private BlockSerializer blockSerializer = mock(BlockSerializer.class);
    private MemPool memPool = mock(MemPool.class);
    private UnconfirmedTransactionProcessingService unconfirmedTransactionProcessingService = mock(UnconfirmedTransactionProcessingService.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(AccountCurrencyTable.class, AccountInfoTable.class, AccountControlPhasingTable.class, AccountAssetTable.class, DexContractTable.class, PublicKeyTableProducer.class,
        BlockchainImpl.class, DaoConfig.class, TableRegistryInitializer.class,
        PropertyProducer.class, TransactionApplier.class, ServiceModeDirProvider.class,
        TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
        GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
        ReferencedTransactionDaoImpl.class,
        TaggedDataTable.class, PropertyBasedFileConfig.class,
        DataTagDao.class, FeeCalculator.class,
        DGSGoodsTable.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        UnconfirmedTransactionEntityRowMapper.class, UnconfirmedTransactionCreator.class,
        TransactionBuilderFactory.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        TaggedDataTimestampDao.class,
        TaggedDataExtendDao.class,
        FullTextConfigImpl.class,
        DirProvider.class, TransactionJsonSerializerImpl.class,
        AplAppStatus.class, PrunableMessageTable.class,
        PhasingPollResultTable.class,
        PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class,
        PhasingVoteTable.class, PhasingPollTable.class,
        AccountLedgerTable.class, DGSPurchaseTable.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class,
        DexOrderTable.class,
        CsvEscaperImpl.class, UnconfirmedTransactionTable.class, AccountService.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(AccountGuaranteedBalanceTable.class), AccountGuaranteedBalanceTable.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(TrimService.class), TrimService.class))
        .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
//            .addBeans(MockBean.of(ftlEngine, FullTextSearchEngine.class)) // prod data test
//            .addBeans(MockBean.of(ftlService, FullTextSearchService.class)) // prod data test
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountServiceImpl.class, AccountService.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyServiceImpl.class, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(AccountTable.class), AccountTable.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(AliasService.class), AliasService.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(ntpTimeConfig.time(), NtpTime.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(peersService, PeersService.class))
        .addBeans(MockBean.of(generatorService, GeneratorService.class))
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(blockSerializer, BlockSerializer.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(unconfirmedTransactionProcessingService, UnconfirmedTransactionProcessingService.class))
        .addBeans(MockBean.of(memPool, MemPool.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .build();

    private List<String> blockIndexExportContent = List.of("block_id,block_height", "1,1", "2,2", "3,30");
    private List<String> transactionIndexExportContent = List.of(
        "transaction_id,partial_transaction_hash,transaction_index,height",
        "100,b'zG8XGTR3IJylgh0305HnCuZo3RwR3XmO',0,30",
        "101,b'InCisA4/cPtdXY4No8eRnt1NM2gXbm8t',0,1",
        "102,b'uW1en2TlHFl1E3F2ke7urxiiaoZANPYs',1,1",
        "103,b'zKWh+CX5uRi+APNUBvcLEItmVrKZdVVY',2,1"
    );
    private List<String> transactionExportContent = List.of(
        "id,deadline,recipient_id,transaction_index,amount,fee,full_hash,height,block_id,signature,timestamp,type,subtype,sender_id,sender_public_key,block_timestamp,referenced_transaction_full_hash,phased,attachment_bytes,version,has_message,has_encrypted_message,has_public_key_announcement,ec_block_height,ec_block_id,has_encrypttoself_message,has_prunable_message,has_prunable_encrypted_message,has_prunable_attachment",
        "3444674909301056677,1440,null,0,0,2500000000000,b'pSSXT5TxzS/MbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=',1000,-468651855371775066,b'N17xwFrlmifvJjNqWa/mkBTGi5v0Nk1bGy+k6+MCAgqGitNl818MqNPrrdxGns06fEnexeTS+tQfZyiXe3MzzA==',35073712,5,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',9200,b'ZAAAAAAAAADMbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=',0,b'AQVmc2RmcwNRRVIFAGZzZGZzAa4VAAAAAAAAAAAAAAAAAACuFQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB',1,0,0,0,14399,-5416619518547901377,0,0,0,0",
        "2402544248051582903,1440,null,1,0,1000000000000,b't8dFrkONVyEicKKwDj9w+11djg2jx5Ge3U0zaBduby0=',1000,-468651855371775066,b'/G8R85aqIHF8kZGh+yX6sGgVEsyXbJNdsVY4mKq62Q/8bO0o4bizOD1au1WSi7sSKmdNwGariwzFhbm0zb2PrA==',35075179,2,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',9200,b'ZQAAAAAAAADMbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=',0,b'AQdNWUFTU0VUCwBmZGZza2RmbGFscxAnAAAAAAAAAg==',1,0,0,0,14405,-2297016555338476945,0,0,0,0",
        "3444674909301056677,1440,null,0,0,2500000000000,b'pSSXT5TxzS/MbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=',1000,-468651855371775066,b'N17xwFrlmifvJjNqWa/mkBTGi5v0Nk1bGy+k6+MCAgqGitNl818MqNPrrdxGns06fEnexeTS+tQfZyiXe3MzzA==',35073712,5,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',9200,b'ZAAAAAAAAADMbxcZNHcgnKWCHTfTkecK5mjdHBHdeY4=',0,b'AQVmc2RmcwNRRVIFAGZzZGZzAa4VAAAAAAAAAAAAAAAAAACuFQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB',1,0,0,0,14399,-5416619518547901377,0,0,0,0",
        "5373370077664349170,1440,457571885748888948,0,100000000000000000,100000000,b'8ovlxZ0Lkkq5bV6fZOUcWXUTcXaR7u6vGKJqhkA09iw=',1500,-7242168411665692630,b'iv06kdDjAR5QXgNTsfcInA1AFnL47V0N3CEH4LEwqgvdF/A7LXXu2PzGRc2oi1yCrBtiHBQqutmxu5XfUXqnDA==',35078473,0,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',13800,b't8dFrkONVyEicKKwDj9w+11djg2jx5Ge3U0zaBduby0=',0,null,1,0,0,0,14734,2621055931824266697,0,0,0,0"
    );
    private List<String> blockTransactionExportContent = List.of(
        "id,deadline,recipient_id,transaction_index,amount,fee,full_hash,height,block_id,signature,timestamp,type,subtype,sender_id,sender_public_key,block_timestamp,referenced_transaction_full_hash,phased,attachment_bytes,version,has_message,has_encrypted_message,has_public_key_announcement,ec_block_height,ec_block_id,has_encrypttoself_message,has_prunable_message,has_prunable_encrypted_message,has_prunable_attachment",
        "2083198303623116770,1440,-1017037002638468431,0,100000000000000000,100000000,b'4vcm5NEB6Rxu5zXJ2g1Vr3EAxFJjoKagkgwlWg9ltE8=',8000,6438949995368593549,b'0kgRvEvixwMRlv0iBjnxiFyOFcludnIUbIjC7qJdigzU6TuOIyTiUi46/xT6oe+BH8Q6lx/b23H3rAtWFOcGyw==',35078473,0,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',73600,b'9X/g0icw8EsBxaExtSCZNWyJmymt2wR22DXqLeXMVpE=',FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE",
        "808614188720864902,1440,-5803127966835594607,1,100000000000000000,100000000,b'hj4MB1LGOAvnY1S9hhvgcFcR4O4rwLhNnw1xtaQnGvY=',8000,6438949995368593549,b'OEhMYSiycHqB6m8MnxlmPbzVQ1jmRNVs+iszY18tVw97kcQYIPjRkj4K/KXLDleFx2wv2FnjVMh2qWQKdYgqog==',35078473,0,0,9211698109297098287,b'vwztBHLYuj354hgI6Y5hs0QEqtc34rrhd4zrxpi0Dzc=',73600,b'4vcm5NEB6Rxu5zXJ2g1Vr3EAxFJjoKagkgwlWg9ltE8=',FALSE,null,1,FALSE,FALSE,FALSE,14734,2621055931824266697,FALSE,FALSE,FALSE,FALSE"
    );
    private List<String> blockExportContent = List.of(
        "id,version,timestamp,previous_block_id,total_amount,total_fee,payload_length,previous_block_hash,cumulative_difficulty,base_target,next_block_id,height,generation_signature,block_signature,payload_hash,generator_id,timeout",
        "6438949995368593549,4,73600,-5580266015477525080,0,200000000,207,b'qBVH25/pjrIk083BIPcwXTuCnxYr6zv3GXUODPSNvp0=',b'At+1GWz0FbA=',23058430050,7551185434952726924,8000,b'Wxv0Y/IC7A1KtCqWNJdu1Ht3xGLR3iXj/qPo6qit2PY=',b'mS6suKw7y7fb2/y2NzGK2rGQ1IQ7ANqJYf0272Bxjw9azKRmLP3PhEfMUR1eNqtMMhwYU4LzV38BBsK/ufgO5g==',b'i9+Y+8TPzwtm36pojOfvkGP4sXSO4jjCPoIJ8HHPzuc=',6415509874415488619,0"
    );

    public CsvExporterTest() throws Exception {
    }

    private static Path createPath(String fileName) {
        try {
            Path folder = temporaryFolderExtension.newFolder().toPath().resolve(fileName);
            Files.createDirectories(folder);
            return folder;
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @BeforeEach
    void setUp() {
        dataExportPath = createPath("csvExportDir");
        csvExporter = new CsvExporterImpl(extension.getDatabaseManager(), dataExportPath, translator);
    }

    @Test
    void exportDerivedTables() throws Exception {
        Collection<DerivedTableInterface<? extends DerivedEntity>> result = registry.getDerivedTables(); // extract all derived tables
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
        extension.cleanAndPopulateDb();

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
            String fileName = ((File) next).getName();
            log.trace("File in folder = {}", fileName);
            int readCount = importCsvAndCheckContent(fileName, dataExportPath);
            assertTrue(readCount > 0);
        }
        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        List<String> lines = Files.readAllLines(shardExportedFile);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("block_timeouts"));
        assertTrue(lines.get(0).contains("block_timestamps"));
        assertFalse(lines.get(0).contains("prunable_zip_hash"));
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
        assertIterableEquals(List.of(transactionIndexExportContent.get(0), transactionIndexExportContent.get(2),
            transactionIndexExportContent.get(3), transactionIndexExportContent.get(4)), transactionIndexCsv);
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
        long exported = csvExporter.exportTransactions( /*two rows exported*/ List.of(td.DB_ID_2, td.DB_ID_0),
            /*more two rows exported*/ 1000);
        assertEquals(4, exported);
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
        assertEquals(1, exported);
        List<String> exportedBlock = Files.readAllLines(dataExportPath.resolve("block.csv"));
        assertEquals(blockExportContent, exportedBlock);
    }

    @Test
    void testExportGoodsSortedByName() throws IOException {
        csvExporter.exportDerivedTableCustomSort(goodsTable, 542100, 2, Set.of("db_id", "latest", "id", "seller_id"), "name");
        List<String> allLines = Files.readAllLines(dataExportPath.resolve("goods.csv"));
        assertEquals(7, allLines.size());
        assertTrue(allLines.get(1).startsWith("\'1"));
        assertTrue(allLines.get(2).startsWith("\'Some product"));
        assertTrue(allLines.get(6).startsWith("\'Test product"));
    }

    @Test
    void testExportDexOfferSortedByHeight() throws IOException {
        csvExporter.exportDerivedTableCustomSort(dexOrderTable, 542100, 2, Set.of("db_id", "latest"), "height");
        List<String> allLines = Files.readAllLines(dataExportPath.resolve("dex_offer.csv"));
        assertEquals(11, allLines.size());
        assertTrue(allLines.get(1).startsWith("1,0,100"));
        assertTrue(allLines.get(2).startsWith("2,1,100"));
        assertTrue(allLines.get(5).startsWith("5,0,100"));
    }

    @Test
    void testExportBlockWhichNotExist() {
        assertThrows(IllegalStateException.class, () -> csvExporter.exportBlock(Integer.MAX_VALUE));
    }

    @Test
    void testExportBlockWithoutTransactions() throws IOException {
        BlockTestData td = new BlockTestData();
        long exported = csvExporter.exportBlock(td.GENESIS_BLOCK.getHeight());
        assertEquals(1, exported);
        assertTrue(Files.exists(dataExportPath.resolve("block.csv")));
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
        doReturn(new MinMaxValue(BigDecimal.ONE, BigDecimal.valueOf(2), "db_id", 2, 2)).when(genesisTable).getMinMaxValue(8000);
        long exported = csvExporter.exportDerivedTable(genesisTable, 8000, 2);
        assertEquals(-1, exported);
    }

    @Test
    void testExportShardTableIgnoringLastHashes() throws IOException, URISyntaxException {
        extension.cleanAndPopulateDb();

        long exportedRows = csvExporter.exportShardTableIgnoringLastZipHashes(4, 1);

        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        assertEquals(2, exportedRows);

        try (Stream<Path> pathStream = Files.list(dataExportPath)) {
            assertEquals(1, pathStream.count());
        }
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
        assertEquals(0, com.apollocurrency.aplwallet.apl.util.FileUtils.countElementsOfDirectory(dataExportPath));
        assertFalse(Files.exists(shardExportedFile));
    }

    @Test
    void testExportShardTableIgnoringLastHashesWhenOnlyOneShardExists() throws IOException, URISyntaxException {
        long exportedRows = csvExporter.exportShardTableIgnoringLastZipHashes(2, 1);
        assertEquals(1, exportedRows);

        Path shardExportedFile = dataExportPath.resolve("shard.csv");
        try (Stream<Path> pathStream = Files.list(dataExportPath)) {
            assertEquals(1, pathStream.count());
        }
        assertTrue(Files.exists(shardExportedFile));

        List<String> lines = Files.readAllLines(shardExportedFile);
        assertEquals(2, lines.size());
        List<String> expectedRows = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource("shard-last-hashes-null.csv").toURI())).subList(0, 2);
        assertEquals(expectedRows, lines);
    }

    private int importCsvAndCheckContent(String itemName, Path dataExportDir) throws Exception {
        int readRowsFromFile = 0;

        // open CSV Reader and read data
        try (CsvReader csvReader = new CsvReaderImpl(dataExportDir, translator);
             ResultSet rs = csvReader.read(itemName, null, null)) {
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
                    log.trace("Row column [{}] value is {}", meta.getColumnLabel(j + 1), object);
                }
                readRowsFromFile++;
            }
        }
        return readRowsFromFile;
    }

    BlockchainConfig mockBlockchainConfig() {
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        HeightConfig config = mock(HeightConfig.class);
        Chain chain = mock(Chain.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        return blockchainConfig;
    }

    private PropertiesHolder mockPropertiesHolder() {
        PropertiesHolder holder = mock(PropertiesHolder.class);
        doReturn(23).when(holder).getIntProperty("apl.derivedTablesCount", 55);
        return holder;
    }
}