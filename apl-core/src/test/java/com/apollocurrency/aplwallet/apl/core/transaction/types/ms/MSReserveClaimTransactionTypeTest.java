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
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSReserveClaimAttachment;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MSReserveClaimTransactionTypeTest {
    public static final long CURRENCY_ID = 1L;
    private final MSReserveClaimAttachment attachment = new MSReserveClaimAttachment(CURRENCY_ID, 1000);
    public static final long SENDER_ID = 1000;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;
    @Mock
    AccountCurrencyService accountCurrencyService;

    @InjectMocks
    MSReserveClaimTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Currency currency;

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_RESERVE_CLAIM, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_RESERVE_CLAIM, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ReserveClaim", type.getName());
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(17);
        buff.put((byte) 1);
        buff.putLong(CURRENCY_ID);
        buff.putLong(1000);
        buff.flip();

        MSReserveClaimAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "MSReserveClaim attachment expected to be of size 17");
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.ReserveClaim", 1);
        json.put("currency", Long.toUnsignedString(CURRENCY_ID));
        json.put("units", 1000L);

        MSReserveClaimAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_notEnoughCurrency() {
        mockAttachment();
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(100L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1000 has not enough 1 currency to claim currency reserve: required 1000, " +
            "but has only 100", ex.getMessage());
        verify(currencyService).validate(currency, tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_OK() {
        mockAttachment();
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(SENDER_ID, CURRENCY_ID)).thenReturn(1000L);

        type.doStateDependentValidation(tx);

        verify(currencyService).validate(currency, tx);
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_zeroUnits() {
        mockAttachment(new MSReserveClaimAttachment(0L, 0));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Reserve claim number of units must be positive: 0", ex.getMessage());
        verifyNoInteractions(accountCurrencyService, accountService, currencyService, blockchainConfig);
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_OK() {
        mockAttachment();

        type.doStateIndependentValidation(tx);

        verifyNoInteractions(accountCurrencyService, accountService, currencyService, blockchainConfig);
    }

    @Test
    void applyAttachmentUnconfirmed_NotEnoughCurrency() {
        mockAttachment();
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(100L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass, when not enough currency balance");
        verify(accountCurrencyService, never()).addToUnconfirmedCurrencyUnits(any(), any(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        mockAttachment();
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, CURRENCY_ID)).thenReturn(1000L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed should pass, when enough currency balance");
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_RESERVE_CLAIM, 0, CURRENCY_ID, -1000);
    }

    @Test
    void undoAttachmentUnconfirmed_noCurrency() {
        mockAttachment();
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(null);

        type.undoAttachmentUnconfirmed(tx, sender);

        verifyNoInteractions(accountCurrencyService);
    }

    @Test
    void undoAttachmentUnconfirmed_WhenCurrencyExist() {
        mockAttachment();
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_RESERVE_CLAIM, 0, CURRENCY_ID, 1000);
    }

    @Test
    void applyAttachment() {
        mockAttachment();

        type.applyAttachment(tx, sender, null);

        verify(currencyService).claimReserve(LedgerEvent.CURRENCY_RESERVE_CLAIM, 0, sender, CURRENCY_ID, 1000);
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "MS Reserve Claim tx should not have recipient");
    }


    private void mockAttachment() {
        mockAttachment(attachment);
    }
    private void mockAttachment(MSReserveClaimAttachment attachment) {
        when(tx.getAttachment()).thenReturn(attachment);
    }

}