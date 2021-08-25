/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuanceAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveIncreaseAttachment;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MSReserveIncreaseTransactionTypeTest {
    public static final long CURRENCY_ID = 1L;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    CurrencyService currencyService;
    @Mock
    Blockchain blockchain;

    @InjectMocks
    MSReserveIncreaseTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Currency currency;

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.MS_RESERVE_INCREASE, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.CURRENCY_RESERVE_INCREASE, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ReserveIncrease", type.getName());
    }

    @Test
    void parseAttachmentFromBytes() throws AplException.NotValidException {
        ByteBuffer attachmentBuff = ByteBuffer.allocate(17);
        attachmentBuff.put((byte) 1);
        attachmentBuff.putLong(CURRENCY_ID);
        attachmentBuff.putLong(1000L);
        attachmentBuff.flip();

        MonetarySystemReserveIncreaseAttachment parsedAttachment = type.parseAttachment(attachmentBuff);

        assertEquals(new MonetarySystemReserveIncreaseAttachment(CURRENCY_ID, 1000L), parsedAttachment);
    }

    @Test
    void parseAttachmentFromJson() throws AplException.NotValidException {
        JSONObject jsonAttachment = new JSONObject();
        jsonAttachment.put("version.ReserveIncrease", CURRENCY_ID);
        jsonAttachment.put("currency", "1");
        jsonAttachment.put("amountPerUnitATM", 1000L);

        MonetarySystemReserveIncreaseAttachment parsedAttachment = type.parseAttachment(jsonAttachment);

        assertEquals(new MonetarySystemReserveIncreaseAttachment(CURRENCY_ID, 1000L), parsedAttachment);
    }

    @Test
    void doStateDependentValidation() throws AplException.ValidationException {
        mockAttachment(50);
        doReturn(currency).when(currencyService).getCurrency(CURRENCY_ID);

        type.doStateDependentValidation(tx);

        verify(currencyService).validate(currency, tx); // will validate reserve overflow inside the CurrencyType.RESERVABLE, see CurrencyTypeReservableTest
    }

    @Test
    void doStateIndependentValidation_zeroReservePerUnit() {
        mockAttachment(0);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Reserve increase amount must be positive: 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationOK() throws AplException.ValidationException {
        mockAttachment(10);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void applyAttachmentUnconfirmedOK() {
        mockAttachment(10);
        doReturn(currency).when(currencyService).getCurrency(CURRENCY_ID);
        doReturn(1000L).when(currency).getReserveSupply();
        doReturn(10_000L).when(sender).getUnconfirmedBalanceATM();

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(result, "Apply attachment unconfirmed should be successful for account balance = 10_000 and total reserve in ATMs 10_000");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_RESERVE_INCREASE, 0, -10_000);
    }

    @Test
    void applyAttachmentUnconfirmed_DoubleSpending() {
        mockAttachment(10);
        doReturn(currency).when(currencyService).getCurrency(CURRENCY_ID);
        doReturn(1000L).when(currency).getReserveSupply();
        doReturn(5_000L).when(sender).getUnconfirmedBalanceATM();

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(result, "Apply attachment unconfirmed should NOT be executed for account balance = 5_000 and total reserve in ATMs 10_000");
        verifyNoInteractions(accountService);
    }

    @Test
    void undoAttachmentUnconfirmed_withCurrency() {
        mockAttachment(10);
        doReturn(currency).when(currencyService).getCurrency(CURRENCY_ID);
        doReturn(150L).when(currency).getReserveSupply();

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_RESERVE_INCREASE, 0, 1500);
    }

    @Test
    void undoAttachmentUnconfirmed_withoutCurrency() {
        mockAttachment(10);
        mockIssuanceTx();

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.CURRENCY_RESERVE_INCREASE, 0, 2500);
    }


    @Test
    void applyAttachment() {
        mockAttachment(5);

        type.applyAttachment(tx, sender, null);

        verify(currencyService).increaseReserve(LedgerEvent.CURRENCY_RESERVE_INCREASE, 0, sender, CURRENCY_ID, 5);
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "RESERVE_INCREASE tx type should not allow recipients");
    }


    private void mockIssuanceTx() {
        Transaction currencyIssuanceTx = mock(Transaction.class);
        doReturn(currencyIssuanceTx).when(blockchain).getTransaction(CURRENCY_ID);
        MonetarySystemCurrencyIssuanceAttachment issuanceAttachment = new MonetarySystemCurrencyIssuanceAttachment(
            "TEST_COIN", "TST", "Test description", (byte) 4, 100L, 250L,
            250L, 1000, 2L, 0, 0, (byte) 0, (byte) 0, (byte) 0);
        doReturn(issuanceAttachment).when(currencyIssuanceTx).getAttachment();
    }

    private void mockAttachment(long amountPerUnitATM) {
        doReturn(new MonetarySystemReserveIncreaseAttachment(CURRENCY_ID, amountPerUnitATM)).when(tx).getAttachment();
    }
}