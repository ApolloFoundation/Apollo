/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dex;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DexOrderTransactionTypeTest {
    private final long senderId = -1;
    private final String ethAddress = "0xfCf7Bca2e5928E9807805e061760F0797bb3C4AC";
    private final String aplAddress = "APL-NLBV-54CH-552P-3VZPQ";
    private final DexOrderAttachmentV2 buyAttachment = new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000L, (byte) 1, 2, (byte) 0, 2220, ethAddress, aplAddress);
    private final DexOrderAttachmentV2 sellAttachment = new DexOrderAttachmentV2((byte) 1, (byte) 0, 1000L, (byte) 1, 2, (byte) 0, 2220, ethAddress, aplAddress);

    @Mock
    TimeService timeService;
    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;
    @Mock
    DexService dexService;

    @InjectMocks
    DexOrderTransactionType type;


    @Mock
    Transaction tx;
    @Mock
    Account sender;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.DEX_ORDER, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        // TODO weird ledger event, which means nothing useful
        assertEquals(LedgerEvent.TRANSACTION_FEE, type.getLedgerEvent());
    }

    @Test
    void parseAttachment_fromBytes() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(95);
        buff.put((byte) 2);//version
        buff.put((byte) 1); // sell type
        buff.put((byte) 0); // order currency apl
        buff.putLong(1000L); // amount
        buff.put((byte) 1); // pair currency eth
        buff.putLong(2); // pair rate
        buff.put((byte) 0); // status open
        buff.putInt(2220); // finish time
        buff.putShort((short) ethAddress.getBytes().length); // from address length
        buff.put(ethAddress.getBytes()); // from address
        buff.putShort((short) aplAddress.getBytes().length); // toAddress length
        buff.put(aplAddress.getBytes()); // toAddress
        buff.flip();

        DexOrderAttachment dexOrderAttachment = type.parseAttachment(buff);

        assertTrue(dexOrderAttachment instanceof DexOrderAttachmentV2, "Parsed from bytes attachment should be of 2 version");
        assertEquals(sellAttachment, dexOrderAttachment);
    }

    @Test
    void parseAttachment_fromJson() throws AplException.NotValidException {
        JSONObject json = new JSONObject();
        json.put("version.DexOrder_v2", 2);
        json.put("type", 1);
        json.put("offerCurrency", 0);
        json.put("offerAmount", 1000L);
        json.put("pairCurrency", 1);
        json.put("pairRate", 2);
        json.put("status", 0);
        json.put("finishTime", 2220);
        json.put("fromAddress", ethAddress);
        json.put("toAddress", aplAddress);

        DexOrderAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(sellAttachment, parsedAttachment);
    }

    @Test
    void doStateDependentValidation_sell_notEnoughApl() {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountService.getAccount(senderId)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(999L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 18446744073709551615 has not enough funds: required 1000, but only has 999", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_sell_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountService.getAccount(senderId)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(1000L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateDependentValidation_buy_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(buyAttachment);

        type.doStateDependentValidation(tx);
    }


    @Test
    void doStateIndependentValidation_equalPairedAndOrderCurrencies() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000L, (byte) 0, 2, (byte) 0, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid Currency codes: 0 / 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_statusIsNotPendingOrOpen() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000L, (byte) 1, 2, (byte) 3, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Expected status 0 (OPEN) or 1 (PENDING) got, 3", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_invalidOrderCurrency() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) -1, 1000L, (byte) 1, 2, (byte) 0, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid dex codes: -1 / 1 / 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_invalidPairedCurrency() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000L, (byte) 3, 2, (byte) 1, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid dex codes: 0 / 3 / 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_invalidOrderType() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 2, (byte) 0, 1000L, (byte) 1, 2, (byte) 1, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid dex codes: 0 / 1 / 2", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroPairRate() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000L, (byte) 1, 0, (byte) 0, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("pairRate should be more than zero.", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroOrderAmount() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 0, (byte) 1, 2, (byte) 0, 2220, ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("offerAmount should be more than zero.", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_v2_blankFromAddress() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000, (byte) 1, 2, (byte) 0, 2220, "   ", aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("fromAddress should be not null and address length less then 110", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_v2_fromAddressTooLong() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000, (byte) 1, 2, (byte) 0, 2220, ethAddress + ethAddress + ethAddress + ethAddress + ethAddress, aplAddress));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("fromAddress should be not null and address length less then 110", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroFinishTime() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000, (byte) 1, 2, (byte) 0, 0, ethAddress, aplAddress));
        when(timeService.getEpochTime()).thenReturn(1000);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("finishTime must be a positive value, but got 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_orderDurationTooLong() {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachmentV2((byte) 0, (byte) 0, 1000, (byte) 1, 2, (byte) 0, 88000, ethAddress, aplAddress));
        when(timeService.getEpochTime()).thenReturn(1000);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("orderDuration 87000 is not in range [1-86400]", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_v2_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(timeService.getEpochTime()).thenReturn(1000);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_v1_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(new DexOrderAttachment((byte) 0, (byte) 0, 1000, (byte) 1, 2, (byte) 0, 2000));
        when(timeService.getEpochTime()).thenReturn(1000);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void applyAttachmentUnconfirmed_sell_notEnoughAplBalance() {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(999L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass, when not enough apl balance");
        verifyNoInteractions(accountService);
    }

    @Test
    void applyAttachmentUnconfirmed_sell_OK() {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(1000L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass, when enough apl balance");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DEX_FREEZE_MONEY, 0, -1000L);
    }

    @Test
    void applyAttachmentUnconfirmed_buy_OK() {
        when(tx.getAttachment()).thenReturn(buyAttachment);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass in any case for buy order");
        verifyNoInteractions(accountService);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(sellAttachment);
        when(tx.getSenderId()).thenReturn(senderId);
        when(tx.getId()).thenReturn(11L);
        when(tx.getHeight()).thenReturn(129);

        type.applyAttachment(tx, sender, null);

        verify(dexService).saveOrder(new DexOrder(0L, 11L, OrderType.SELL, senderId, DexCurrency.APL, 1000L, DexCurrency.ETH, new BigDecimal(BigInteger.valueOf(2), 9), 2220, OrderStatus.OPEN, 129, ethAddress, aplAddress));
    }

    @Test
    void undoAttachmentUnconfirmed_sell() {
        when(tx.getAttachment()).thenReturn(sellAttachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DEX_FREEZE_MONEY, 0L, 1000);
    }

    @Test
    void undoAttachmentUnconfirmed_buy() {
        when(tx.getAttachment()).thenReturn(buyAttachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verifyNoInteractions(accountService);
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "DexOrder tx type can not have a recipient");
    }

    @Test
    void isPhasingSafe() {
        assertFalse(type.isPhasingSafe(), "DexOrder tx type is not phasing safe");
    }

    @Test
    void getName() {
        assertEquals("DexOrder", type.getName());
    }
}