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
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSExchangeSellAttachment;
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
class MSExchangeSellTransactionTypeTest {
    private static final long CURRENCY_ID = 9999L;
    private final MSExchangeSellAttachment attachment = new MSExchangeSellAttachment(CURRENCY_ID, 10, 1200);
    private static final long SENDER_ID = 1L;
    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    ExchangeRequestService exchangeRequestService;
    @Mock
    CurrencyExchangeOfferFacade exchangeOfferFacade;
    @Mock
    CurrencyService currencyService;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;

    @InjectMocks
    MSExchangeSellTransactionType type;

    @Mock
    Currency currency;
    @Mock
    Account sender;
    @Mock
    Transaction tx;

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_SELL, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_EXCHANGE_SELL, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ExchangeSell", type.getName());
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(25);
        buff.put((byte) 1); //version
        buff.putLong(CURRENCY_ID); //currency id
        buff.putLong(10); // rate
        buff.putLong(1200); // units
        buff.flip();

        MSExchangeSellAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "MSExchangeSellAttachment should be of size 25");
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.ExchangeSell", 1);
        json.put("currency", Long.toUnsignedString(CURRENCY_ID));
        json.put("rateATM", 10L);
        json.put("units", 1200L);

        MSExchangeSellAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void doStateDependentValidation_notEnoughCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(1199L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough 9999 currency to place currency  exchange sell order: " +
            "required 1200, but has only 1199", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(1200L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void applyAttachmentUnconfirmed_noEnoughCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(1199L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass when not enough currency");
        verify(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, CURRENCY_ID);
        verifyNoMoreInteractions(accountCurrencyService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(1200L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass when enough currency");
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_EXCHANGE_SELL, 0, CURRENCY_ID, -1200);
    }

    @Test
    void undoAttachmentUnconfirmed_noCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(null);

        type.undoAttachmentUnconfirmed(tx, sender);

        verifyNoInteractions(accountService, accountCurrencyService);
    }

    @Test
    void undoAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_EXCHANGE_SELL, 0, CURRENCY_ID, 1200);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.applyAttachment(tx, sender, null);

        verify(exchangeRequestService).addExchangeRequest(tx, attachment);
        verify(exchangeOfferFacade).exchangeCurrencyForAPL(tx, sender, CURRENCY_ID, 10, 1200);
    }
}