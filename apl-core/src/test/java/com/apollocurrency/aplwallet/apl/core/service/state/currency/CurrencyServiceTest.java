/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyBuyOfferServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TransactionValidationHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MSCurrencyIssuanceTransactionType;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyMintTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private DatabaseManager databaseManager = mock(DatabaseManager.class);
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, CurrencyBuyOfferTable.class, CurrencyBuyOfferServiceImpl.class
    )
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(blockChainInfoService, BlockChainInfoService.class))
        .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
        .build();

    CurrencyService service;
    CurrencyTestData td;
    BlockTestData blockTestData;
    @Mock
    private CurrencySupplyTable currencySupplyTable;
    @Mock
    private CurrencyTable currencyTable;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountCurrencyService accountCurrencyService;
    @Mock
    private CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    @Mock
    private CurrencyFounderService currencyFounderService;
    @Mock
    private ExchangeService exchangeService;
    @Mock
    private CurrencyTransferService currencyTransferService;
    @Mock
    private ShufflingService shufflingService;
    @Mock
    private IteratorToStreamConverter<CurrencyTransfer> iteratorToStreamConverter;
    @Mock
    private CurrencyBuyOfferService buyOfferService;
    @Mock
    private MonetaryCurrencyMintingService monetaryCurrencyMintingService;
    @Mock
    CurrencyMintTable currencyMintTable;
    @Mock
    private FullTextSearchUpdater fullTextSearchUpdater;
    @Mock
    private FullTextSearchService fullTextSearchService;

    @Mock
    TransactionValidationHelper transactionValidationHelper;

    @BeforeEach
    void setUp() {
        td = new CurrencyTestData();
        service = new CurrencyServiceImpl(currencySupplyTable, currencyTable, currencyMintTable, monetaryCurrencyMintingService, blockChainInfoService,
            accountService, accountCurrencyService, currencyExchangeOfferFacade, currencyFounderService,
            exchangeService, currencyTransferService, shufflingService, blockchainConfig,
            transactionValidationHelper, fullTextSearchUpdater, fullTextSearchService);
    }

    @Test
    void addCurrency() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        MonetarySystemCurrencyIssuance attach = mock(MonetarySystemCurrencyIssuance.class);
        doReturn("ANY_CODE").when(attach).getCode();
        doReturn("ANY_NAME").when(attach).getName();
        doReturn(null).doReturn(null).when(currencyTable).getBy(any(DbClause.StringClause.class));
        doReturn("currency").when(currencyTable).getTableName();

        //WHEN
        service.addCurrency(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), tr, account, attach);

        //THEN
        verify(currencyTable).insert(any(Currency.class));
        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
    }

    @Test
    void increaseReserve() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        doReturn(td.CURRENCY_0.getAccountId()).when(account).getId();
        doReturn(td.CURRENCY_0).when(currencyTable).get(any(DbKey.class));

        //WHEN
        service.increaseReserve(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account, td.CURRENCY_0.getId(), 0L);

        //THEN
        verify(accountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong());
        verify(currencySupplyTable).insert(any(CurrencySupply.class));
        verify(currencyFounderService).addOrUpdateFounder(anyLong(), anyLong(), anyLong());
    }

    @Test
    void claimReserve() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        doReturn(td.CURRENCY_0).when(currencyTable).get(any(DbKey.class));

        //WHEN
        service.claimReserve(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account, td.CURRENCY_0.getId(), -1L);

        //THEN
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong());
        verify(currencySupplyTable).insert(any(CurrencySupply.class));
    }

    @Test
    void transferCurrency() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);

        //WHEN
        service.transferCurrency(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account, account, td.CURRENCY_2.getId(), 1L);

        //THEN
        verify(accountCurrencyService).addToCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(accountCurrencyService).addToCurrencyAndUnconfirmedCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

    @Test
    void loadCurrencySupplyByCurrency() {
        CurrencySupply result = service.loadCurrencySupplyByCurrency(td.CURRENCY_0);
        assertEquals(td.CURRENCY_0.getCurrencySupply(), result);
    }

    @Test
    void increaseSupply() {
        //WHEN
        service.increaseSupply(td.CURRENCY_0, 1L);
        //THEN
        verify(currencySupplyTable).insert(any(CurrencySupply.class));
    }

    @Test
    void canBeDeletedBy() {
        //WHEN
        service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        verify(accountCurrencyService).getCurrenciesByAccount(td.CURRENCY_0.getId(), 0, -1);
    }

    @Test
    void delete() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        Stream<CurrencyBuyOffer> buyOffers = Stream.of(mock(CurrencyBuyOffer.class));
        doReturn(buyOffers).when(buyOfferService).getOffersStream(td.CURRENCY_3, 0, -1);
        doReturn(buyOfferService).when(currencyExchangeOfferFacade).getCurrencyBuyOfferService();
        doReturn("currency").when(currencyTable).getTableName();
        //WHEN
        service.delete(td.CURRENCY_3, LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account);
        //THEN
        verify(buyOfferService).getOffersStream(any(Currency.class), anyInt(), anyInt());
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(accountCurrencyService).addToCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(currencyTable).deleteAtHeight(any(Currency.class), anyInt());
        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
    }

    @Test
    void validate() throws Exception {
        //GIVEN
        Transaction tx = mock(Transaction.class);
        doReturn(new MSCurrencyIssuanceTransactionType(blockchainConfig, accountService, mock(CurrencyService.class), accountCurrencyService)).when(tx).getType();
        MonetarySystemCurrencyIssuance attachment = new MonetarySystemCurrencyIssuance("ff", "CC", "Test currency", (byte) 1, 1000, 0, 1000, 0, 0, 0, 0, (byte) 0, (byte) 0, (byte) 2);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(10L).when(config).getMaxBalanceATM();

        //WHEN
        service.validate(td.CURRENCY_3, td.CURRENCY_3.getType(), tx);
    }

    @Test
    void validateCurrencyNaming() throws Exception {
        MonetarySystemCurrencyIssuance issuance = mock(MonetarySystemCurrencyIssuance.class);
        doReturn("Name").when(issuance).getName();
        doReturn("CODE").when(issuance).getCode();
        doReturn("Description").when(issuance).getDescription();
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn("APL").when(blockchainConfig).getCoinSymbol();

        //WHEN
        service.validateCurrencyNamingStateDependent(td.CURRENCY_3.getAccountId(), issuance);
        service.validateCurrencyNamingStateIndependent(issuance);
    }


    @Test
    void mintCurrency() {
        AccountTestData accountTestData = new AccountTestData();
        CurrencyMintTestData currencyMintTestData = new CurrencyMintTestData();
        //GIVEN
        MonetarySystemCurrencyMinting attachment = mock(MonetarySystemCurrencyMinting.class);
        doReturn(100L).when(attachment).getCounter();
        doReturn(td.CURRENCY_3.getId()).when(attachment).getCurrencyId();
        doReturn(td.CURRENCY_3.getMinReservePerUnitATM()).when(attachment).getUnits();
        Account account = mock(Account.class);
        doReturn(accountTestData.ACC_4.getId()).when(account).getId();
        doReturn(currencyMintTestData.CURRENCY_MINT_3).when(currencyMintTable).get(any(DbKey.class));
        td.CURRENCY_3.setType(20);
        doReturn(td.CURRENCY_3).when(currencyTable).get(new LongKey(td.CURRENCY_3.getId()));
        doReturn(true).when(monetaryCurrencyMintingService).meetsTarget(
            anyLong(), any(Currency.class), any(MonetarySystemCurrencyMinting.class));
        LedgerEvent ledgerEvent = mock(LedgerEvent.class);

        //WHEN
        service.mintCurrency(ledgerEvent, currencyMintTestData.CURRENCY_MINT_4.getCurrencyId(), account, attachment);

        //THEN
        verify(currencyMintTable).get(any(DbKey.class));
        verify(currencyMintTable).insert((any(CurrencyMint.class)));
        verify(currencySupplyTable).insert(any(CurrencySupply.class));
    }

    @Test
    void getCounter() {
        //GIVEN
        CurrencyMintTestData currencyMintTestData = new CurrencyMintTestData();
        doReturn(currencyMintTestData.CURRENCY_MINT_4).when(currencyMintTable).get(any(DbKey.class));
        //WHEN
        long result = service.getMintCounter(currencyMintTestData.CURRENCY_MINT_4.getCurrencyId(), currencyMintTestData.CURRENCY_MINT_4.getAccountId());
        assertEquals(currencyMintTestData.CURRENCY_MINT_4.getCounter(), result);
        //THEN
        verify(currencyMintTable).get(any(DbKey.class));
    }

    @Test
    void deleteCurrency() {
        blockTestData = new BlockTestData();
        CurrencyMintTestData currencyMintTestData = new CurrencyMintTestData();
        //GIVEN
        DbIterator<CurrencyMint> dbIt = mock(DbIterator.class);
        doReturn(true).doReturn(true).doReturn(false).when(dbIt).hasNext();
        doReturn(currencyMintTestData.CURRENCY_MINT_3).doReturn(currencyMintTestData.CURRENCY_MINT_2).when(dbIt).next();
        doReturn(dbIt).when(currencyMintTable).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(blockTestData.BLOCK_10.getHeight()).when(blockChainInfoService).getHeight();
        Currency currency = mock(Currency.class);
        doReturn(100L).when(currency).getId();

        //WHEN
        ((CurrencyServiceImpl) service).deleteMintingCurrency(currency);

        //THEN
        verify(currencyMintTable).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
    }
}