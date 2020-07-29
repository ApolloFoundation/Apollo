package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
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
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);

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

    @BeforeEach
    void setUp() {
        td = new CurrencyTestData();
        service = new CurrencyServiceImpl(currencySupplyTable, currencyTable, blockChainInfoService,
            accountService, accountCurrencyService, currencyExchangeOfferFacade, currencyFounderService,
            exchangeService, currencyTransferService, shufflingService, blockchainConfig);
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

        //WHEN
        service.addCurrency(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), tr, account, attach);

        //THEN
        verify(currencyTable).insert(any(Currency.class));
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

    @Test // can't be implemented while old static Shuffling is NOT refactored
    void canBeDeletedBy() {
        //WHEN
        service.canBeDeletedBy(td.CURRENCY_0, 1L);
    }

    @Test // can't be implemented while old static Shuffling is NOT refactored
    void delete() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        //WHEN
//        service.delete(td.CURRENCY_3, LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account);
    }

    @Test
    void validate() {
    }

    @Test
    void validateCurrencyNaming() {
    }
}