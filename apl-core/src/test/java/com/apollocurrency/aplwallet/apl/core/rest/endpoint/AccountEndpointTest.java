package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyBasedFileConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.*;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.tagged.TaggedDataServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Setter;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountEndpointTest extends AbstractEndpointTest{

    private static final String PASSPHRASE="123456";

    //private AccountService accountService = mock(AccountService.class);

    @RegisterExtension
    private DbExtension extension = new DbExtension();

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    private NtpTime time = mock(NtpTime.class);
    private KeyStoreService keyStore = new VaultKeyStoreServiceImpl(temporaryFolderExtension.newFolder("keystorePath").toPath(), time);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainImpl.class,
            DaoConfig.class,
            PropertyProducer.class, TransactionApplier.class,// DirProvider.class, //ServiceModeDirProvider.class,
            TrimService.class, AccountTable.class,
            JdbiHandleFactory.class,
            TaggedDataServiceImpl.class, TransactionValidator.class, TransactionProcessorImpl.class,
            GlobalSyncImpl.class, DefaultBlockValidator.class, ReferencedTransactionService.class,
            ReferencedTransactionDaoImpl.class,
            TaggedDataDao.class,
            PropertyBasedFileConfig.class,
            DataTagDao.class, PhasingPollServiceImpl.class, PhasingPollResultTable.class,
            PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class, PhasingVoteTable.class, PhasingPollTable.class,
            KeyFactoryProducer.class, FeeCalculator.class, AplAppStatus.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            AccountController.class, AccountBalanceService.class, AccountConverter.class, WalletKeysConverter.class,
            AccountServiceImpl.class, AccountTable.class,
            AccountInfoServiceImpl.class, AccountInfoTable.class,
            AccountLeaseServiceImpl.class, AccountLeaseTable.class,
            AccountAssetServiceImpl.class, AccountAssetTable.class,
            AccountPublicKeyServiceImpl.class, PublicKeyTable.class, GenesisPublicKeyTable.class,
            AccountCurrencyServiceImpl.class, AccountCurrencyTable.class,
            AccountPropertyServiceImpl.class, AccountPropertyTable.class
    )
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
            .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
            .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessorImpl.class, BlockchainProcessor.class))
            .addBeans(MockBean.of(keyStore, KeyStoreService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(AccountGuaranteedBalanceTable.class, AccountGuaranteedBalanceTable.class))
            .build();


    @Inject @Setter
    private AccountController endpoint;

    @BeforeEach
    void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(endpoint);

        //endpoint.setConverter(new AccountConverter());
        //endpoint.setAccountService(accountService);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void createAccount() throws URISyntaxException, IOException {
        //when(accountService.findOrCreatePeerByAddress(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendPostRequest("/accounts/account", "passphrase="+PASSPHRASE);

        //print("response=%s", response);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        WalletKeysInfoDTO dto = mapper.readValue(content, WalletKeysInfoDTO.class);
        assertNotNull(dto);
        assertEquals(PASSPHRASE, dto.getPassphrase());
        assertEquals(dto.getAccountRS(), dto.getApl().getWallets().getAccountRS());
    }


}