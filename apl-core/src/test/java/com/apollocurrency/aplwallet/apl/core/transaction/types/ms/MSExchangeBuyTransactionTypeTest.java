/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
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
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MSExchangeBuyTransactionTypeTest {
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
        MonetarySystemExchangeBuyAttachment attachment = new MonetarySystemExchangeBuyAttachment(1L, 10L, 5);
        ByteBuffer buff = ByteBuffer.allocate(25);
        buff.put((byte) 1); //version
        buff.putLong(1L);  // currency id
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
        attachmentJson.put("units", 2L);

        MonetarySystemExchangeBuyAttachment parsedAttachment = type.parseAttachment(attachmentJson);

        assertEquals(new MonetarySystemExchangeBuyAttachment(-1, 10, 2), parsedAttachment);
    }

    @Test
    void applyAttachmentUnconfirmedOK() {
        doReturn(new MonetarySystemExchangeBuyAttachment(-1, 10, 2)).when(tx).getAttachment();
        doReturn(25L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "Apply unconfirmed operation should be successful for account with balance of 25 and order total 20 ");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_EXCHANGE_BUY, 0L, -20L);
    }

    @Test
    void applyAttachmentUnconfirmedDoubleSpending() {
        doReturn(new MonetarySystemExchangeBuyAttachment(-1, 10, 2)).when(tx).getAttachment();
        doReturn(19L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "Apply unconfirmed operation should not be successful for account with balance of 19 and order total 20 ");
        verifyNoInteractions(accountService);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        doReturn(new MonetarySystemExchangeBuyAttachment(1, 10, 5)).when(tx).getAttachment();

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_EXCHANGE_BUY, 0, 50L);
    }

    @Test
    void applyAttachment() {
        MonetarySystemExchangeBuyAttachment attachment = new MonetarySystemExchangeBuyAttachment(1, 10, 5);
        doReturn(attachment).when(tx).getAttachment();

        type.applyAttachment(tx, sender, null);

        verify(exchangeRequestService).addExchangeRequest(tx, attachment);
        verify(offerFacade).exchangeAPLForCurrency(tx, sender, 1, 10, 5);

    }
}