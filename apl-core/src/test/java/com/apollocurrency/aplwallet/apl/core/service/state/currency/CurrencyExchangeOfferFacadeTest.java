/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyBuyOfferServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyExchangeOfferFacadeImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyBuyOfferTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencySellOfferTestData;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class CurrencyExchangeOfferFacadeTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        CurrencyBuyOfferTable.class, CurrencyBuyOfferServiceImpl.class
    )
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(blockChainInfoService, BlockChainInfoService.class))
        .addBeans(MockBean.of(Mockito.mock(TimeService.class), TimeService.class, TimeServiceImpl.class))
        .addBeans(MockBean.of(Mockito.mock(PropertiesHolder.class), PropertiesHolder.class))
        .addBeans(MockBean.of(Mockito.mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .build();

    @Mock
    CurrencyBuyOfferService currencyBuyOfferService;
    @Mock
    CurrencySellOfferService currencySellOfferService;
    @Mock
    AccountService accountService;
    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    ExchangeService exchangeService;

    CurrencyExchangeOfferFacade currencyExchangeOfferFacade;

    private CurrencyBuyOfferTestData tdBuy;
    private CurrencySellOfferTestData tdSell;
    private AccountTestData tdAccount;
    private BlockTestData tdBlock;

    @BeforeEach
    void setUp() {
        tdBuy = new CurrencyBuyOfferTestData();
        tdSell = new CurrencySellOfferTestData();
        tdAccount = new AccountTestData();
        tdBlock = new BlockTestData();
        currencyExchangeOfferFacade = spy(
            new CurrencyExchangeOfferFacadeImpl(
                currencyBuyOfferService, currencySellOfferService, blockChainInfoService, accountService, accountCurrencyService, exchangeService));
    }

    @Test
    void test_publishOffer() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        doReturn(tdBuy.OFFER_7.getAccountId()).when(tr).getSenderId();
        MonetarySystemPublishExchangeOffer attach = mock(MonetarySystemPublishExchangeOffer.class);
        doReturn(tdBuy.OFFER_7.getCurrencyId()).when(attach).getCurrencyId();
        doReturn(tdBuy.OFFER_7).when(currencyBuyOfferService).getOffer(tdBuy.OFFER_7.getCurrencyId(), tdBuy.OFFER_7.getAccountId());
        doReturn(tdSell.OFFER_5).when(currencySellOfferService).getOffer(tdBuy.OFFER_7.getId());
        doReturn(tdAccount.ACC_5).when(accountService).getAccount(tdBuy.OFFER_7.getAccountId());

        //WHEN
        currencyExchangeOfferFacade.publishOffer(tr, attach);

        //THEN
        verify(currencyExchangeOfferFacade).removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, tdBuy.OFFER_7);
        verify(currencyBuyOfferService).remove(tdBuy.OFFER_7);
        verify(currencySellOfferService).remove(tdSell.OFFER_5);
        verify(accountService).getAccount(tdBuy.OFFER_7.getAccountId());
        verify(accountService).addToUnconfirmedBalanceATM(
            tdAccount.ACC_5, LedgerEvent.CURRENCY_OFFER_REPLACED,
            tdBuy.OFFER_7.getId(),
            Math.multiplyExact(tdBuy.OFFER_7.getSupply(), tdBuy.OFFER_7.getRateATM()));

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(
            tdAccount.ACC_5, LedgerEvent.CURRENCY_OFFER_REPLACED,
            tdBuy.OFFER_7.getId(), tdBuy.OFFER_7.getCurrencyId(), tdSell.OFFER_5.getSupply());
        verify(currencyBuyOfferService).addOffer(tr, attach);
        verify(currencySellOfferService).addOffer(tr, attach);
    }

    @Test
    void test_removeOffer() {
        //GIVEN
        doReturn(tdSell.OFFER_5).when(currencySellOfferService).getOffer(tdBuy.OFFER_7.getId());
        doReturn(tdAccount.ACC_5).when(accountService).getAccount(tdBuy.OFFER_7.getAccountId());

        //WHEN
        currencyExchangeOfferFacade.removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, tdBuy.OFFER_7);

        //THEN
        verify(currencyExchangeOfferFacade).removeOffer(LedgerEvent.CURRENCY_OFFER_REPLACED, tdBuy.OFFER_7);
        verify(currencyBuyOfferService).remove(tdBuy.OFFER_7);
        verify(currencySellOfferService).remove(tdSell.OFFER_5);
        verify(accountService).getAccount(tdBuy.OFFER_7.getAccountId());
        verify(accountService).addToUnconfirmedBalanceATM(
            tdAccount.ACC_5, LedgerEvent.CURRENCY_OFFER_REPLACED,
            tdBuy.OFFER_7.getId(),
            Math.multiplyExact(tdBuy.OFFER_7.getSupply(), tdBuy.OFFER_7.getRateATM()));

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(
            tdAccount.ACC_5, LedgerEvent.CURRENCY_OFFER_REPLACED,
            tdBuy.OFFER_7.getId(), tdBuy.OFFER_7.getCurrencyId(), tdSell.OFFER_5.getSupply());
    }

    @Test
    void test_calculateTotal_buyOffers() {
        //GIVEN
        List<CurrencyExchangeOffer> offers = List.of(tdBuy.OFFER_0, tdBuy.OFFER_1, tdBuy.OFFER_2);

        //WHEN
        AvailableOffers availableOffers = currencyExchangeOfferFacade.calculateTotal(offers, 3L);

        //THEN
        assertNotNull(availableOffers);
        assertEquals(3, availableOffers.getUnits());
        assertEquals(3, availableOffers.getAmountATM());
    }

    @Test
    void test_calculateTotal_sellOffers() {
        //GIVEN
        List<CurrencyExchangeOffer> offers = List.of(tdSell.OFFER_0, tdSell.OFFER_1, tdSell.OFFER_2);

        //WHEN
        AvailableOffers availableOffers = currencyExchangeOfferFacade.calculateTotal(offers, 3L);

        //THEN
        assertNotNull(availableOffers);
        assertEquals(3, availableOffers.getUnits());
        assertEquals(3, availableOffers.getAmountATM());
    }

    @Test
    void test_getAvailableToBuy() {
        List<CurrencyExchangeOffer> offers = List.of(tdSell.OFFER_3, tdSell.OFFER_4, tdSell.OFFER_5);
        doReturn(offers).when(currencyExchangeOfferFacade).getAvailableSellOffers(
            tdSell.OFFER_3.getCurrencyId(), 0);

        //WHEN
        AvailableOffers availableOffers = currencyExchangeOfferFacade
            .getAvailableToBuy(tdSell.OFFER_3.getCurrencyId(), 1L);

        //THEN
        assertNotNull(availableOffers);
        verify(currencyExchangeOfferFacade).getAvailableSellOffers(
            tdSell.OFFER_3.getCurrencyId(), 0);
    }

    @Test
    void test_getAvailableToSell() {
        List<CurrencyExchangeOffer> offers = List.of(tdBuy.OFFER_5, tdBuy.OFFER_6, tdSell.OFFER_7);
        doReturn(offers).when(currencyExchangeOfferFacade).getAvailableBuyOffers(
            tdBuy.OFFER_3.getCurrencyId(), 0);

        //WHEN
        AvailableOffers availableOffers = currencyExchangeOfferFacade
            .getAvailableToSell(tdBuy.OFFER_3.getCurrencyId(), 1L);

        //THEN
        assertNotNull(availableOffers);
        verify(currencyExchangeOfferFacade).getAvailableBuyOffers(
            tdBuy.OFFER_3.getCurrencyId(), 0);
    }

    @Test
    void test_exchangeCurrencyForAPL() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        List<CurrencyExchangeOffer> offers = List.of(tdBuy.OFFER_4, tdBuy.OFFER_5, tdBuy.OFFER_6, tdBuy.OFFER_7, tdBuy.OFFER_8);
        doReturn(offers).when(currencyExchangeOfferFacade).getAvailableBuyOffers(
            tdBuy.OFFER_3.getCurrencyId(), 1);
        doReturn(tdAccount.ACC_5).doReturn(tdAccount.ACC_4).doReturn(tdAccount.ACC_3).doReturn(tdAccount.ACC_2).doReturn(tdAccount.ACC_1)
            .when(accountService).getAccount(anyLong());
        doReturn(tdAccount.ACC_5).when(accountService).getAccount(any(Account.class));
        doReturn(tdSell.OFFER_8).doReturn(tdSell.OFFER_7).doReturn(tdSell.OFFER_6).doReturn(tdSell.OFFER_5).doReturn(tdSell.OFFER_4)
            .when(currencySellOfferService).getOffer(anyLong());
        doReturn(tdBlock.BLOCK_8).when(blockChainInfoService).getLastBlock();

        //WHEN
        currencyExchangeOfferFacade.exchangeCurrencyForAPL(tr, tdAccount.ACC_5, tdBuy.OFFER_3.getCurrencyId(), 1L, 5);

        //THEN
        verify(currencyBuyOfferService, times(5)).insert(any(CurrencyBuyOffer.class));
        verify(currencySellOfferService, times(5)).insert(any(CurrencySellOffer.class));
        verify(currencySellOfferService, times(5)).getOffer(anyLong());
        verify(accountService, times(5)).getAccount(anyLong());
        verify(accountService, times(5))
            .addToBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong());
        verify(exchangeService, times(5))
            .addExchange(any(Transaction.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), /*any(Block.class), */anyLong());

        verify(accountService, times(1)).getAccount(any(Account.class));
        verify(accountService, times(1))
            .addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong());
        verify(accountCurrencyService, times(6))
            .addToCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(accountCurrencyService, times(6))
            .addToUnconfirmedCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

    @Test
    void test_exchangeAPLForCurrency() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        doReturn(tdBuy.OFFER_3.getId()).when(tr).getId();
        List<CurrencyExchangeOffer> offers = List.of(tdSell.OFFER_4, tdSell.OFFER_5, tdSell.OFFER_6, tdSell.OFFER_7, tdSell.OFFER_8);
        doReturn(offers).when(currencyExchangeOfferFacade).getAvailableSellOffers(
            tdBuy.OFFER_3.getCurrencyId(), 1);
        doReturn(tdAccount.ACC_5).doReturn(tdAccount.ACC_4).doReturn(tdAccount.ACC_3).doReturn(tdAccount.ACC_2).doReturn(tdAccount.ACC_1)
            .when(accountService).getAccount(anyLong());
        doReturn(tdAccount.ACC_5).when(accountService).getAccount(any(Account.class));
        doReturn(tdBuy.OFFER_8).doReturn(tdBuy.OFFER_7).doReturn(tdBuy.OFFER_6).doReturn(tdBuy.OFFER_5).doReturn(tdBuy.OFFER_4)
            .when(currencyBuyOfferService).getOffer(anyLong());
        doReturn(tdBlock.BLOCK_8).when(blockChainInfoService).getLastBlock();

        //WHEN
        currencyExchangeOfferFacade.exchangeAPLForCurrency(tr, tdAccount.ACC_5, tdBuy.OFFER_3.getCurrencyId(), 1L, 5);

        //THEN
        verify(currencyBuyOfferService, times(5)).insert(any(CurrencyBuyOffer.class));
        verify(currencySellOfferService, times(5)).insert(any(CurrencySellOffer.class));
        verify(currencyBuyOfferService, times(5)).getOffer(anyLong());
        verify(accountService, times(5)).getAccount(anyLong());
        verify(accountService, times(6))
            .addToBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong());
        verify(accountService, times(1)).getAccount(any(Account.class));
        verify(accountCurrencyService, times(5))
            .addToCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
        verify(exchangeService, times(5))
            .addExchange(any(Transaction.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), /*any(Block.class), */anyLong());
        verify(accountCurrencyService, times(1))
            .addToCurrencyAndUnconfirmedCurrencyUnits(
                any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

}