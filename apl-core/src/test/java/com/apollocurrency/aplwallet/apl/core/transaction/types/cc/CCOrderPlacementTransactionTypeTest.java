/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCOrderPlacementTransactionTypeTest {

    public static final long ASSET_ID = 1L;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    AssetService assetService;

    @InjectMocks
    TestCCOrderPlacementTransactionType type;

    // supporting mocks
    @Mock
    Transaction tx;
    @Mock
    HeightConfig heightConfig;

    @Test
    void doStateDependentValidation_noAsset() {
        mockAttachment(10, 2);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Asset 1 does not exist yet", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_orderQuantityIsGreaterThanAllowedByAsset() {
        mockAttachment(10, 2);
        Asset asset = mock(Asset.class);
        doReturn(asset).when(assetService).getAsset(ASSET_ID);
        doReturn(9L).when(asset).getInitialQuantityATU();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Invalid asset order placement asset or quantity: " +
            "{\"priceATM\":2,\"quantityATU\":10,\"asset\":\"1\",\"version.TestCCOrderPlacement\":1}", ex.getMessage());
    }

    @Test
    void doStateDependentValidationOK() throws AplException.ValidationException {
        CCOrderPlacementAttachment attachment1 = mockAttachment(10, 2);
        Asset asset = mock(Asset.class);
        doReturn(asset).when(assetService).getAsset(ASSET_ID);
        doReturn(20L).when(asset).getInitialQuantityATU();

        type.doStateDependentValidation(tx);

        verify(assetService).getAsset(ASSET_ID);
    }


    @Test
    void doStateIndependentValidation_zeroPrice() {
        mockAttachment(5, 0);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset order placement: " +
            "{\"priceATM\":0,\"quantityATU\":5,\"asset\":\"1\",\"version.TestCCOrderPlacement\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroQuantity() {
        mockAttachment(0, 1);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset order placement: " +
            "{\"priceATM\":1,\"quantityATU\":0,\"asset\":\"1\",\"version.TestCCOrderPlacement\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_priceExceedsMaxBalance() {
        mockAttachment(20, 10);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(9L).when(heightConfig).getMaxBalanceATM();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset order placement: " +
            "{\"priceATM\":10,\"quantityATU\":20,\"asset\":\"1\",\"version.TestCCOrderPlacement\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_assetIdIsZero() {
        mockAttachment(0, 20, 10);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(10L).when(heightConfig).getMaxBalanceATM();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset order placement: " +
            "{\"priceATM\":10,\"quantityATU\":20,\"asset\":\"0\",\"version.TestCCOrderPlacement\":1}", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_txIsInTheOverflowValidationSkipList_OK() {
        mockAttachment(1, 20, 10);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(10L).when(heightConfig).getMaxBalanceATM();
        when(tx.getId()).thenReturn(1000L);
        when(blockchainConfig.isTotalAmountOverflowTx(1000L)).thenReturn(true);

        type.doStateIndependentValidation(tx);
    }
    @Test
    void doStateIndependentValidation_orderTotalOverflow() {
        mockAttachment( 20, Long.MAX_VALUE / 19);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(Long.MAX_VALUE).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Result of multiplying x=20, y=485440633518672410 exceeds the allowed range [-9223372036854775808;9223372036854775807]," +
            " transaction='null', type='null', sender='0'", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_orderTotalIsGreaterThanMaxBalanceAllowed() {
        mockAttachment( 20, 10);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(190L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Order total in ATMs 200 is greater than max allowed: 190"
            + ", asset=1, quantity=20, price=10", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationOK() throws AplException.ValidationException {
        mockAttachment( 20, 10);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(200L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();

        type.doStateIndependentValidation(tx);

        verify(tx).getType();
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "CC_ORDER_PLACEMENT tx types should not have recipient");
    }

    @Test
    void isPhasingSafe() {
        assertTrue(type.isPhasingSafe(), "CC_ORDER_PLACEMENT tx types should support phasing safe");
    }

    private CCOrderPlacementAttachment mockAttachment(long assetId, long quantity, long price) {
        CCOrderPlacementAttachment attachment = new TestCCOrderPlacementAttachment(assetId, quantity, price);
        doReturn(attachment).when(tx).getAttachment();
        return attachment;
    }
    private CCOrderPlacementAttachment mockAttachment(long quantity, long price) {
        return mockAttachment(ASSET_ID, quantity, price);
    }

    private static class TestCCOrderPlacementTransactionType extends CCOrderPlacementTransactionType {
        public TestCCOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService) {
            super(blockchainConfig, accountService, assetService);
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

    private static class TestCCOrderPlacementAttachment extends CCOrderPlacementAttachment {
        public TestCCOrderPlacementAttachment(long assetId, long quantityATU, long priceATM) {
            super(assetId, quantityATU, priceATM);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
            return null;
        }

        @Override
        public String getAppendixName() {
            return "TestCCOrderPlacement";
        }
    }
}