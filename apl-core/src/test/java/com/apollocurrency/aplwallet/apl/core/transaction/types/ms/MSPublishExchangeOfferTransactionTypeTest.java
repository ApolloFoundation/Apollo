/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MSPublishExchangeOfferTransactionTypeTest {
    private static final long CURRENCY_ID = 1L;
    private static final long SENDER_ID = 9999L;
    private final MSPublishExchangeOfferAttachment attachment = new MSPublishExchangeOfferAttachment(CURRENCY_ID, 12, 15, 1000,
        2000, 200, 300, 1111);

    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    CurrencyExchangeOfferFacade exchangeOfferFacade;
    @Mock
    TransactionValidator transactionValidator;
    @Mock
    CurrencyService currencyService;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig config;

    @InjectMocks
    MSPublishExchangeOfferTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Currency currency;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("PublishExchangeOffer", type.getName());
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(61);
        buff.put((byte) 1); // version
        buff.putLong(1L); // currency id
        buff.putLong(12L); // buy rate
        buff.putLong(15L); // sell rate
        buff.putLong(1000L); // buy limit
        buff.putLong(2000L); // sell limit
        buff.putLong(200L); // initial buy supply
        buff.putLong(300L); // initial sell supply
        buff.putInt(1111); // expiration height
        buff.flip();

        MSPublishExchangeOfferAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.PublishExchangeOffer", 1);
        json.put("currency", Long.toUnsignedString(1L));
        json.put("buyRateATM", 12L);
        json.put("sellRateATM", 15L);
        json.put("totalBuyLimit", 1000L);
        json.put("totalSellLimit", 2000L);
        json.put("initialBuySupply", 200L);
        json.put("initialSellSupply", 300L);
        json.put("expirationHeight", 1111);

        MSPublishExchangeOfferAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_notActiveCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(false);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Currency not currently active: {\"totalSellLimit\":2000,\"sellRateATM\":15," +
            "\"initialSellSupply\":300,\"totalBuyLimit\":1000,\"expirationHeight\":1111," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":200,\"buyRateATM\":12}",
            ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_notEnoughApl() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getFeeATM()).thenReturn(100L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2499L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 9999 has not enough funds: required 2500, but only has 2499", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_notEnoughCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getFeeATM()).thenReturn(100L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2500L);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(299L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 9999 has not enough 1 currency to publish currency  exchange offer:" +
            " required 300, but has only 299", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getFeeATM()).thenReturn(100L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2500L);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(300L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_buyRateIsZero() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 0, 100, 1000, 2000, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, buy rate 0 and sell rate 100 has to be larger than 0, " +
            "buy rate cannot be larger than sell rate", ex.getMessage());
    }
    @Test
    void doStateIndependentValidation_sellRateIsZero() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 0, 1000, 2000, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, buy rate 1 and sell rate 0 has to be larger than 0, " +
            "buy rate cannot be larger than sell rate", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_sellRateLessThanBuyRate() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 2, 1, 1000, 2000, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, buy rate 2 and sell rate 1 has to be larger than 0, " +
            "buy rate cannot be larger than sell rate", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalBuyLimitIsNegative() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, -1, 2000, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, units and height cannot be negative: {\"totalSellLimit\":2000," +
            "\"sellRateATM\":2,\"initialSellSupply\":30,\"totalBuyLimit\":-1,\"expirationHeight\":100," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":20,\"buyRateATM\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalSellLimitIsNegative() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 1000, -1, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, units and height cannot be negative: {\"totalSellLimit\":-1," +
            "\"sellRateATM\":2,\"initialSellSupply\":30,\"totalBuyLimit\":1000,\"expirationHeight\":100," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":20,\"buyRateATM\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalInitialBuySupplyIsNegative() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 1000, 2000, -1, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, units and height cannot be negative: {\"totalSellLimit\":2000," +
            "\"sellRateATM\":2,\"initialSellSupply\":30,\"totalBuyLimit\":1000,\"expirationHeight\":100," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":-1,\"buyRateATM\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalInitialSellSupplyIsNegative() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 1000, 2000, 20, -1, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, units and height cannot be negative: {\"totalSellLimit\":2000," +
            "\"sellRateATM\":2,\"initialSellSupply\":-1,\"totalBuyLimit\":1000,\"expirationHeight\":100," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":20,\"buyRateATM\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_expirationHeightIsNegative() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 1000, 2000, 20, 30, -1));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange offer, units and height cannot be negative: {\"totalSellLimit\":2000," +
            "\"sellRateATM\":2,\"initialSellSupply\":30,\"totalBuyLimit\":1000,\"expirationHeight\":-1," +
            "\"version.PublishExchangeOffer\":1,\"currency\":\"1\",\"initialBuySupply\":20,\"buyRateATM\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalBuyLimitLessThanInitialBuySupply() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 19, 2000, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Initial supplies must not exceed total limits", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalSellLimitLessThanInitialSellSupply() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 1000, 29, 20, 30, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Initial supplies must not exceed total limits", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_totalSellLimitAndTotalBuyLimitAreZero() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 0, 0, 0, 0, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Total buy and sell limits cannot be both 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_initialSellSupplyAndInitialBuySupplyAreZero() {
        when(tx.getAttachment()).thenReturn(
            new MSPublishExchangeOfferAttachment(1, 1, 2, 0, 2000, 0, 0, 100));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Initial buy and sell supply cannot be both 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_expirationHeightNotExceedExecutionHeight() {
        MSPublishExchangeOfferAttachment attachment = new MSPublishExchangeOfferAttachment(1, 1, 2, 0, 2000, 0, 1, 100);
        when(tx.getAttachment()).thenReturn(attachment);
        when(transactionValidator.getFinishValidationHeight(tx, attachment)).thenReturn(100);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Expiration height must be after transaction execution height", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_orderInitialBuyATMsOverflow() {
        MSPublishExchangeOfferAttachment attachment = new MSPublishExchangeOfferAttachment(1, 2, 2, Long.MAX_VALUE, 0, Long.MAX_VALUE, 0, 100);
        when(tx.getAttachment()).thenReturn(attachment);
        when(transactionValidator.getFinishValidationHeight(tx, attachment)).thenReturn(99);
        when(tx.getType()).thenReturn(type);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Result of multiplying x=9223372036854775807, y=2 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], transaction='null', type='MS_PUBLISH_EXCHANGE_OFFER', sender='0'", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_onlySell_OK() {
        MSPublishExchangeOfferAttachment attachment = new MSPublishExchangeOfferAttachment(1, 1, 2, 0, 1000, 0, 100, 100);
        when(tx.getAttachment()).thenReturn(attachment);
        when(transactionValidator.getFinishValidationHeight(tx, attachment)).thenReturn(99);
        when(tx.getType()).thenReturn(type);

        type.doStateIndependentValidation(tx);

        verifyNoInteractions(accountCurrencyService, accountService, currencyService, config, exchangeOfferFacade);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughAplForInitialBuy() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2399L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should fail, when not enough apls to cover initial buy");
        verifyNoInteractions(accountCurrencyService);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughCurrencyForInitialSell() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2400L);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(14L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should fail, when not enough currency to cover initial sell");
        verify(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, CURRENCY_ID);
        verifyNoMoreInteractions(accountCurrencyService, accountService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(2400L);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(300L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed should not fail, when enough currency to cover initial sell and apls to cover initial buy");
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, 0, CURRENCY_ID, -300);
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, 0L, -2400);
        verifyNoMoreInteractions(accountCurrencyService, accountService);
    }

    @Test
    void undoAttachmentUnconfirmed_NoCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(null);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, 0, 2400);
        verifyNoInteractions(accountCurrencyService);
    }

    @Test
    void undoAttachmentUnconfirmed_withCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, 0, 2400);
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_PUBLISH_EXCHANGE_OFFER, 0L, CURRENCY_ID, 300);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.apply(tx, sender, null);

        verify(exchangeOfferFacade).publishOffer(tx, attachment);
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "MSPublishExchangeOffer transaction must not have a recipient");
    }
}