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
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MSExchangeTransactionTypeTest {
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;
    @InjectMocks
    TestMSExchangeTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    HeightConfig heightConfig;

    @Test
    void doStateDependentValidation_currencyIsNotActive() {
        setUpAttachmentMock();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Currency not active: {\"rateATM\":25,\"version.TestMSExchangeAttachment\":1,\"currency\":\"1\",\"units\":10}", ex.getMessage());
        verify(currencyService).isActive(null);
        verify(currencyService).getCurrency(1L);
    }

    private Attachment setUpAttachmentMock() {
        return doReturn(new TestMSExchangeAttachment(1L, 25L, 10L)).when(tx).getAttachment();
    }

    @Test
    void doStateDependentValidationOK() throws AplException.ValidationException {
        setUpAttachmentMock();
        Currency currency = mock(Currency.class);
        doReturn(currency).when(currencyService).getCurrency(1L);
        doReturn(true).when(currencyService).isActive(currency);

        type.doStateDependentValidation(tx);

        verify(currencyService).isActive(currency);
        verify(currencyService).getCurrency(1L);
    }

    @Test
    void doStateIndependentValidationZeroRate() {
        doReturn(new TestMSExchangeAttachment(1L, 0L, 10L)).when(tx).getAttachment();

        AplException.NotValidException ex =
            assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange: {\"rateATM\":0,\"version.TestMSExchangeAttachment\":1," +
            "\"currency\":\"1\",\"units\":10}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationZeroUnits() {
        doReturn(new TestMSExchangeAttachment(1L, 10L, 0L)).when(tx).getAttachment();

        AplException.NotValidException ex =
            assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid exchange: {\"rateATM\":10,\"version.TestMSExchangeAttachment\":1," +
            "\"currency\":\"1\",\"units\":0}", ex.getMessage());
    }

    @Test
    void doStateIndepedentValidation_CurrencySellTxIsChosenByConfigToSkipStrictOverflowValidation_OK() throws AplException.ValidationException {
        doReturn(new TestMSExchangeAttachment(1L, 10L, Long.MAX_VALUE)).when(tx).getAttachment();
        when(tx.getId()).thenReturn(1000L);
        when(blockchainConfig.isCurrencySellTx(1000L)).thenReturn(true);

        type.doStateIndependentValidation(tx);

        verify(blockchainConfig).isCurrencySellTx(1000L);
        verifyNoMoreInteractions(blockchainConfig);
    }

    @Test
    void doStateIndependentValidationOrderTotalOverflow() {
        doReturn(new TestMSExchangeAttachment(1L, 10L, Long.MAX_VALUE)).when(tx).getAttachment();
        doReturn(type).when(tx).getType();

        AplException.NotValidException ex =
            assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Result of multiplying x=10, y=9223372036854775807 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], transaction='null', type='null', sender='0'", ex.getMessage());
        verify(blockchainConfig).isCurrencySellTx(0);
        verifyNoMoreInteractions(blockchainConfig);
    }

    @Test
    void doStateIndependentValidationMaxBalanceExceeded() {
        setUpAttachmentMock();
        doReturn(type).when(tx).getType();
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(249L).when(heightConfig).getMaxBalanceATM();

        AplException.NotValidException ex =
            assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Currency order total in ATMs: 250 is higher than max allowed: 249" +
            ", currency=1, quantity=10, price=25", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationOK() throws AplException.ValidationException {
        setUpAttachmentMock();
        doReturn(type).when(tx).getType();
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(250L).when(heightConfig).getMaxBalanceATM();

        type.doStateIndependentValidation(tx);

        verify(blockchainConfig).getCurrentConfig();
        verify(heightConfig).getMaxBalanceATM();
    }

    @Test
    void canHaveRecipient() {
        boolean haveRecipient = type.canHaveRecipient();

        assertFalse(haveRecipient, "Currency exchange types should not have recipient");
    }

    private static class TestMSExchangeTransactionType extends MSExchangeTransactionType {
        public TestMSExchangeTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService) {
            super(blockchainConfig, accountService, currencyService);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getSpec() {
            return null;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return null;
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE) void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public String getName() {
            return null;
        }
    }

    private static class TestMSExchangeAttachment extends MonetarySystemExchangeAttachment {
        public TestMSExchangeAttachment(long currencyId, long rateATM, long units) {
            super(currencyId, rateATM, units);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
            return null;
        }

        @Override
        public String getAppendixName() {
            return "TestMSExchangeAttachment";
        }
    }
}