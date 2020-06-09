package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import javax.inject.Inject;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyExchangeOfferFacadeImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyBuyOfferTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencySellOfferTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyExchangeOfferFacadeTest {

    @Mock
    CurrencyBuyOfferService currencyBuyOfferService;
    @Mock
    CurrencySellOfferService currencySellOfferService;
    @Mock
    AccountService accountService;
    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    Blockchain blockchain = mock(BlockchainImpl.class);
    @Mock
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @Mock
    BlockChainInfoService blockChainInfoService;
    @Mock
    BlockchainProcessor blockchainProcessor;

    @Inject
    CurrencyExchangeOfferFacade currencyExchangeOfferFacade;

    private CurrencyBuyOfferTestData tdBuy;
    private CurrencySellOfferTestData tdSell;
    private AccountTestData tdAccount;

    @BeforeEach
    void setUp() {
        tdBuy = new CurrencyBuyOfferTestData();
        tdSell = new CurrencySellOfferTestData();
        tdAccount = new AccountTestData();
        currencyExchangeOfferFacade = spy(
            new CurrencyExchangeOfferFacadeImpl(
                currencyBuyOfferService, currencySellOfferService, blockChainInfoService, accountService, accountCurrencyService));
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


}