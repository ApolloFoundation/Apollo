/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MSExchangeBuyTransactionTypeTest {
    private static final long CURRENCY_ID = -1;
    private static final long SENDER_ID = -1000;
    private final MonetarySystemExchangeBuyAttachment attachment = new MonetarySystemExchangeBuyAttachment(CURRENCY_ID, 10, 5);
    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;
    @Mock
    ExchangeRequestService exchangeRequestService;
    @Mock
    CurrencyExchangeOfferFacade offerFacade;

    @InjectMocks
    MSExchangeBuyTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Currency currency;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_BUY, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_EXCHANGE_BUY, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ExchangeBuy", type.getName());
    }

    @Test
    void parseAttachmentFromBuffer() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(25);
        buff.put((byte) 1); //version
        buff.putLong(-1L);  // currency id
        buff.putLong(10L); // rate
        buff.putLong(5L); // units
        buff.flip();

        MonetarySystemExchangeBuyAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void parseAttachmentFromJson() throws AplException.NotValidException {
        JSONObject attachmentJson = new JSONObject();
        attachmentJson.put("version.ExchangeBuy", 1);
        attachmentJson.put("currency", Long.toUnsignedString(-1));
        attachmentJson.put("rateATM", 10L);
        attachmentJson.put("units", 5L);

        MonetarySystemExchangeBuyAttachment parsedAttachment = type.parseAttachment(attachmentJson);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void applyAttachmentUnconfirmedOK() {
        doReturn(attachment).when(tx).getAttachment();
        doReturn(55L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "Apply unconfirmed operation should be successful for account with balance of 55 and order total 50 ");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_EXCHANGE_BUY, 0L, -50L);
    }

    @Test
    void applyAttachmentUnconfirmedDoubleSpending() {
        doReturn(attachment).when(tx).getAttachment();
        doReturn(49L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "Apply unconfirmed operation should not be successful for account with balance of 49 and order total 50 ");
        verifyNoInteractions(accountService);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        doReturn(attachment).when(tx).getAttachment();

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_EXCHANGE_BUY, 0, 50L);
    }

    @Test
    void applyAttachment() {
        doReturn(attachment).when(tx).getAttachment();

        type.applyAttachment(tx, sender, null);

        verify(exchangeRequestService).addExchangeRequest(tx, attachment);
        verify(offerFacade).exchangeAPLForCurrency(tx, sender, -1, 10, 5);

    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_noEnoughAplBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(49L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 18446744073709550616 has not enough funds: required 50, but only has 49", ex.getMessage());
        verify(currencyService).validate(currency, tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(50L);

        type.doStateDependentValidation(tx);

        verify(currencyService).validate(currency, tx);
    }
}