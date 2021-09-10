/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyMintTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdaterImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyBuyOfferServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuanceAttachment;
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;

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
    private FullTextSearchUpdater fullTextSearchUpdater = mock(FullTextSearchUpdater.class);

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
        .addBeans(MockBean.of(fullTextSearchUpdater, FullTextSearchUpdater.class))
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
    private FullTextSearchService fullTextSearchService;
    @Mock
    private Event<FullTextOperationData> fullTextOperationDataEvent;
    @Mock
    TransactionValidationHelper transactionValidationHelper;

    @BeforeEach
    void setUp() {
        td = new CurrencyTestData();
        service = new CurrencyServiceImpl(currencySupplyTable, currencyTable, currencyMintTable, monetaryCurrencyMintingService, blockChainInfoService,
            accountService, accountCurrencyService, currencyExchangeOfferFacade, currencyFounderService,
            exchangeService, currencyTransferService, shufflingService, blockchainConfig,
            transactionValidationHelper, fullTextSearchUpdater, fullTextOperationDataEvent, fullTextSearchService);
    }

    @Test
    void addCurrency() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        MonetarySystemCurrencyIssuanceAttachment attach = mock(MonetarySystemCurrencyIssuanceAttachment.class);
        doReturn("ANY_CODE").when(attach).getCode();
        doReturn("ANY_NAME").when(attach).getName();
        doReturn(null).doReturn(null).when(currencyTable).getBy(any(DbClause.StringClause.class));
        doReturn("currency").when(currencyTable).getTableName();
        Event mockEvent = mock(Event.class);
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(mockEvent);
        when(mockEvent.fireAsync(any())).thenReturn(new CompletableFuture());

        //WHEN
        service.addCurrency(LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), tr, account, attach);

        //THEN
        verify(currencyTable).insert(any(Currency.class));
//        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
        verify(fullTextOperationDataEvent).select(new AnnotationLiteral<TrimEvent>() {});
    }

    @Test
    void increaseReserve() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        Account account = mock(Account.class);
        doReturn(td.CURRENCY_0.getAccountId()).when(account).getId();
        doReturn(td.CURRENCY_0).when(currencyTable).get(anyLong());

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
        doReturn(td.CURRENCY_0).when(currencyTable).get(anyLong());

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
    void loadCurrencySupplyByCurrency_forCurrencyWithAlreadyLoadedSupply() {
        CurrencySupply result = service.loadCurrencySupplyByCurrency(td.CURRENCY_0);

        assertEquals(td.CURRENCY_0.getCurrencySupply(), result);
    }

    @Test
    void loadCurrencySupplyByCurrency_forCurrencyWithoutLoadedAndStoredSupply() {
        CurrencySupply result = service.loadCurrencySupplyByCurrency(td.CURRENCY_2);

        assertEquals(td.CURRENCY_2.getMaxSupply(), result.getCurrentSupply());
    }

    @Test
    void loadCurrencySupplyByCurrency_forCurrencyWithoutLoadedSupplyButWithStored() {
        CurrencySupply savedSupply = new CurrencySupply(td.CURRENCY_2.getId(), 1000, 0, 200, true, false);
        doReturn(savedSupply).when(currencySupplyTable).get(td.CURRENCY_2.getId());

        CurrencySupply result = service.loadCurrencySupplyByCurrency(td.CURRENCY_2);

        assertEquals(savedSupply, result);
        assertEquals(savedSupply, td.CURRENCY_2.getCurrencySupply());
    }


    @Test
    void increaseSupply_burnAll() {
        //WHEN
        service.increaseSupply(td.CURRENCY_2, -td.CURRENCY_2.getInitialSupply());
        //THEN
        verify(currencySupplyTable).insert(new CurrencySupply(td.CURRENCY_2.getId(), 0L, 0, 0, true, false));
    }

    @Test
    void increaseSupply_exceedLimits() {
        // exceed by 1 unit
        assertThrows(IllegalArgumentException.class, ()-> service.increaseSupply(td.CURRENCY_2, 1));
        // burn more than max supply
        assertThrows(IllegalArgumentException.class, ()-> service.increaseSupply(td.CURRENCY_2, -td.CURRENCY_2.getMaxSupply() - 1));

        assertEquals(td.CURRENCY_2.getInitialSupply(), td.CURRENCY_2.getCurrencySupply().getCurrentSupply());
        verify(currencySupplyTable, never()).insert(any(CurrencySupply.class));
    }

    @Test
    void canBeDeletedBy_noHolders() {
        //GIVEN
        doReturn(Collections.emptyList()).when(accountCurrencyService).getByCurrency(td.CURRENCY_0.getId(), 0, -1);
        //WHEN
        boolean canBeDeleted = service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        assertTrue(canBeDeleted, "CURRENCY_0 should allow deletion when no holders left");
        verify(accountCurrencyService).getByCurrency(td.CURRENCY_0.getId(), 0, -1);
    }

    @Test
    void canNotBeDeleted_manyHolders() {
        //GIVEN
        List<AccountCurrency> holders = List.of(
            new AccountCurrency(1L, td.CURRENCY_0.getId(), 10, 10, td.CURRENCY_0.getHeight() + 10),
            new AccountCurrency(2L, td.CURRENCY_0.getId(), 30, 15, td.CURRENCY_0.getHeight() + 11)
            );
        doReturn(holders).when(accountCurrencyService).getByCurrency(td.CURRENCY_0.getId(), 0, -1);
        //WHEN
        boolean canBeDeleted = service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        assertFalse(canBeDeleted, "CURRENCY_0 should not allow deletion when more than one holder left ()");
    }

    @Test
    void canNotBeDeleted_oneNotSenderHolder() {
        //GIVEN
        List<AccountCurrency> holders = List.of(
            new AccountCurrency(2L, td.CURRENCY_0.getId(), 30, 15, td.CURRENCY_0.getHeight() + 11)
        );
        doReturn(holders).when(accountCurrencyService).getByCurrency(td.CURRENCY_0.getId(), 0, -1);
        //WHEN
        boolean canBeDeleted = service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        assertFalse(canBeDeleted, "CURRENCY_0 should not allow deletion when sender's account does not hold the whole currency allocation");
    }

    @Test
    void canBeDeleted_senderIsOnlyOneHolder() {
        //GIVEN
        List<AccountCurrency> holders = List.of(
            new AccountCurrency(1L, td.CURRENCY_0.getId(), 30, 15, td.CURRENCY_0.getHeight() + 11)
        );
        doReturn(holders).when(accountCurrencyService).getByCurrency(td.CURRENCY_0.getId(), 0, -1);
        //WHEN
        boolean canBeDeleted = service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        assertTrue(canBeDeleted, "CURRENCY_0 should allow deletion when only the sender's account hold the whole currency allocation");
    }

    @Test
    void canBeDeleted_atHardcodedHeight_byAnyAccount() {
        //GIVEN
        List<AccountCurrency> holders = List.of();
        // Note, that here is incorrect method invocation, there is no account specified by the currency id, it's an error due to refactoring;
        // but it is required for the backward compatibility
        // Such scenario must be avoided when possible
        doReturn(holders).when(accountCurrencyService).getByAccount(td.CURRENCY_0.getId(), 0, -1);
        doReturn(true).when(blockchainConfig).isCurrencyIssuanceHeight(220);
        doReturn(220).when(blockChainInfoService).getHeight();
        //WHEN
        boolean canBeDeleted = service.canBeDeletedBy(td.CURRENCY_0, 1L);
        //THEN
        assertTrue(canBeDeleted, "CURRENCY_0 should allow deletion for the HARDCODED HEIGHT scenario");
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
        Event mockEvent = mock(Event.class);
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(mockEvent);
        when(mockEvent.fireAsync(any())).thenReturn(new CompletableFuture());

        //WHEN
        service.delete(td.CURRENCY_3, LedgerEvent.CURRENCY_ISSUANCE, tr.getId(), account);
        //THEN
        verify(buyOfferService).getOffersStream(any(Currency.class), anyInt(), anyInt());
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(accountCurrencyService).addToCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(currencyTable).deleteAtHeight(any(Currency.class), anyInt());
//        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
        verify(fullTextOperationDataEvent).select(new AnnotationLiteral<TrimEvent>() {});
    }

    @Test
    void validate() throws Exception {
        //GIVEN
        Transaction tx = mock(Transaction.class);
        doReturn(new MSCurrencyIssuanceTransactionType(blockchainConfig, accountService, mock(CurrencyService.class), accountCurrencyService)).when(tx).getType();
        MonetarySystemCurrencyIssuanceAttachment attachment = new MonetarySystemCurrencyIssuanceAttachment("ff", "CC", "Test currency", (byte) 1, 1000, 0, 1000, 0, 0, 0, 0, (byte) 0, (byte) 0, (byte) 2);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(10L).when(config).getMaxBalanceATM();

        //WHEN
        service.validate(td.CURRENCY_3, td.CURRENCY_3.getType(), tx);
    }

    @Test
    void validateCurrencyNaming() throws Exception {
        MonetarySystemCurrencyIssuanceAttachment issuance = mock(MonetarySystemCurrencyIssuanceAttachment.class);
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
        doReturn(td.CURRENCY_3).when(currencyTable).get(td.CURRENCY_3.getId());
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

    @Test
    void burn_currencyWithFullNotStoredSupply() {
        td.CURRENCY_3.setCurrencySupply(null);
        doReturn(td.CURRENCY_3).when(currencyTable).get(td.CURRENCY_3.getId());
        Account sender = new Account(1222L, 1000L, 800L, 0L, 0L, 0);
        doReturn(200).when(blockChainInfoService).getHeight();

        service.burn(td.CURRENCY_3.getId(), sender, 100, -1);

        verify(currencySupplyTable).insert(new CurrencySupply(td.CURRENCY_3.getId(), 2099999900, 0, 200, true, false));
        verify(accountCurrencyService).addToCurrencyUnits(sender, LedgerEvent.CURRENCY_BURNING, -1, td.CURRENCY_3.getId(), -100);
    }
}