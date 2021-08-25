/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyBurningAttachment;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MSCurrencyBurningTransactionTypeTest {
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    HeightConfig heightConfig;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;
    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    Transaction transaction;
    @InjectMocks
    MSCurrencyBurningTransactionType type;

    Account sender = new Account(1001, 9000, 9000, 0, 0, 200);


    @Test
    void transactionTypeDescription() {
        assertEquals(LedgerEvent.CURRENCY_BURNING, type.getLedgerEvent());
        assertEquals("CurrencyBurning", type.getName());
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_BURNING, type.getSpec());
        assertFalse(type.canHaveRecipient(), "Currency burning transaction should not have recipient");
        assertFalse(type.isPhasingSafe(), "Currency burning transaction should not be phasing safe");
        assertTrue(type.isPhasable(), "Currency burning transaction may be phasable");
    }

    @Test
    void verifyFee() {
        doReturn(100L).when(blockchainConfig).getOneAPL();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(BigDecimal.valueOf(150)).when(heightConfig).getBaseFee(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_BURNING, BigDecimal.ONE);

        assertEquals(15000, type.getBaselineFee(transaction).getFee(transaction, new MonetarySystemCurrencyBurningAttachment(1, 20)));
    }

    @Test
    void parseAttachment_FromJson() throws AplException.NotValidException {

        JSONObject jsonAttachment = new JSONObject(Map.of("units", "10023", "currency", "1092", "version.CurrencyBurning", 1));

        MonetarySystemCurrencyBurningAttachment parsed = type.parseAttachment(jsonAttachment);

        MonetarySystemCurrencyBurningAttachment expected = new MonetarySystemCurrencyBurningAttachment(1092, 10023);
        assertEquals(expected, parsed);
        assertEquals(expected.getFullSize(), parsed.getFullSize());
        assertEquals(17, parsed.getFullSize());

        ByteBuffer byteBuffer = ByteBuffer.allocate(17);
        parsed.putBytes(byteBuffer);
        byteBuffer.rewind();

        MonetarySystemCurrencyBurningAttachment fromBytesAttachment = type.parseAttachment(byteBuffer);

        assertEquals(expected, fromBytesAttachment);
    }

    @Test
    void parseAttachment_FromBytes() throws AplException.NotValidException {

        JSONObject jsonAttachment = new JSONObject(Map.of("units", "10023", "currency", "1092", "version.CurrencyBurning", 1));

        MonetarySystemCurrencyBurningAttachment parsed = type.parseAttachment(jsonAttachment);

        MonetarySystemCurrencyBurningAttachment expected = new MonetarySystemCurrencyBurningAttachment(1092, 10023);
        assertEquals(expected, parsed);
        assertEquals(expected.getFullSize(), parsed.getFullSize());
    }


    @Test
    void doStateDependentValidation_OK() throws AplException.ValidationException {
        mockAttachment(987, 10);
        doReturn(true).when(currencyService).isActive(null);

        type.doStateDependentValidation(transaction);

        verify(currencyService).validate(null, transaction);
    }

    @Test
    void doStateDependentValidation_Failed_CurrencyIsNotActive() throws AplException.ValidationException {
        mockAttachment(987, 10);

        assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateDependentValidation(transaction));

        verify(currencyService).validate(null, transaction);
    }


    @Test
    void doStateIndependentValidation_OK() throws AplException.ValidationException {
        mockAttachment(122, 1);

        type.doStateIndependentValidation(transaction);

        verifyNoInteractions(currencyService, accountCurrencyService, accountService, blockchainConfig);
    }

    @Test
    void doStateIndependentValidation_failed_zeroCurrencyUnits() {
        mockAttachment(10281098, 0);

        assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(transaction));

        verifyNoInteractions(currencyService, accountCurrencyService, accountService, blockchainConfig);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        mockAttachment(100, 20);
        doReturn(20L).when(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, 100);

        boolean applied = type.applyAttachmentUnconfirmed(transaction, sender);

        assertTrue(applied, "Currency Burning transaction should be appliedUnconfirmed, because account with the id=1001 has 20 unconfirmed units of currency, and tx gonna burn these 20 units");
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_BURNING, 0, 100, -20);
    }

    @Test
    void applyAttachmentUnconfirmed_Failed() {
        mockAttachment(100, 20);
        doReturn(19L).when(accountCurrencyService).getUnconfirmedCurrencyUnits(sender, 100);

        boolean applied = type.applyAttachmentUnconfirmed(transaction, sender);

        assertFalse(applied, "Currency Burning transaction should not be appliedUnconfirmed, because account with the id=1001 has 19 unconfirmed units of currency but tx gonna burn 20 units");
        verify(accountCurrencyService, never()).addToUnconfirmedCurrencyUnits(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

    @Test
    void undoAttachmentUnconfirmed() {
        mockAttachment(100, 20);

        type.undoAttachmentUnconfirmed(transaction, sender);

        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(sender, LedgerEvent.CURRENCY_BURNING, 0, 100, 20);
    }

    @Test
    void applyAttachment() {
        mockAttachment(1L, 2000);

        type.applyAttachment(transaction, sender, null);

        verify(currencyService).burn(1, sender, 2000, 0);
    }

    private void mockAttachment(long currencyId, long units) {
        MonetarySystemCurrencyBurningAttachment attachment = new MonetarySystemCurrencyBurningAttachment(currencyId, units);
        doReturn(attachment).when(transaction).getAttachment();
    }
}