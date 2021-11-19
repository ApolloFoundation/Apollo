/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSCurrencyTransferAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class MSCurrencyTransferTransactionTypeTest {
    private final long currencyId = -1;
    private final long units = 100;
    private final long senderId = -1000;
    private final MSCurrencyTransferAttachment attachment = new MSCurrencyTransferAttachment(currencyId, units);
    private final long genesisAccountId = 999L;

    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    CurrencyTransferService currencyTransferService;
    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;

    @InjectMocks
    MSCurrencyTransferTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Account recipient;
    @Mock
    Currency currency;

    // Mock and set back original static field for genesis creator id
    private long originalGenesisCreatorId;

    @BeforeEach
    void setUp() {
        originalGenesisCreatorId = GenesisImporter.CREATOR_ID;
        GenesisImporter.CREATOR_ID = 999L;
    }

    @AfterEach
    void tearDown() {
        GenesisImporter.CREATOR_ID = originalGenesisCreatorId;
    }

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_TRANSFER, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_TRANSFER, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("CurrencyTransfer", type.getName());
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(17);
        buff.put((byte) 1);
        buff.putLong(currencyId);
        buff.putLong(units);
        buff.flip();

        MSCurrencyTransferAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "MSCurrencyTransfer attachment should be of size 17");
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.CurrencyTransfer", 1);
        json.put("currency", Long.toUnsignedString(currencyId));
        json.put("units", units);

        MSCurrencyTransferAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_currencyIsNotActive() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(currencyId)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(false);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Currency not currently active: {\"version.CurrencyTransfer\":1,\"currency\":" +
            "\"18446744073709551615\",\"units\":100}", ex.getMessage());
        verify(currencyService).validate(currency, tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_notEnoughCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(currencyId)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderId, currencyId)).thenReturn(99L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 18446744073709550616 has not enough currency 18446744073709551615 to perform " +
            "transfer: required 100, but has only 99", ex.getMessage());
        verify(currencyService).validate(currency, tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(currencyId)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderId, currencyId)).thenReturn(100L);

        type.doStateDependentValidation(tx);

        verify(currencyService).validate(currency, tx);
    }

    @Test
    void doStateIndependentValidation_zeroUnitsTransfer() {
        when(tx.getAttachment()).thenReturn(new MSCurrencyTransferAttachment(currencyId, 0));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid currency transfer: {\"version.CurrencyTransfer\":1,\"currency\":" +
            "\"18446744073709551615\",\"units\":0}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_transferToGenesisAccount() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getRecipientId()).thenReturn(genesisAccountId);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Currency transfer to genesis account not allowed", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getRecipientId()).thenReturn(1L);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, currencyId)).thenReturn(99L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass when not enough currency on the balance");
        verify(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, currencyId);
        verifyNoMoreInteractions(accountCurrencyService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(sender, currencyId)).thenReturn(100L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed should pass when  enough currency on the balance");
        verify(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, currencyId);
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_TRANSFER, 0L, currencyId, -units);
    }

    @Test
    void undoAttachmentUnconfirmed_noCurrency() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(currencyId)).thenReturn(null);

        type.undoAttachmentUnconfirmed(tx, sender);

        verifyNoInteractions(accountCurrencyService);
    }

    @Test
    void undoAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(currencyService.getCurrency(currencyId)).thenReturn(currency);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_TRANSFER, 0L, currencyId, units);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.applyAttachment(tx, sender, recipient);

        verify(currencyService).transferCurrency(LedgerEvent.CURRENCY_TRANSFER, 0, sender, recipient, currencyId, units);
        verify(currencyTransferService).addTransfer(tx, attachment);
    }

    @Test
    void canHaveRecipient() {
        assertTrue(type.canHaveRecipient(), "MSCurrencyTransfer should have a recipient");
    }
}