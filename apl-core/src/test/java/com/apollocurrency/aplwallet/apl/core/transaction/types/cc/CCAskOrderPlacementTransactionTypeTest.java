/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCAskOrderPlacementTransactionTypeTest {
    private final long assetId = -1;
    private final long quantity = 100;
    private final long price = 3;
    private final long senderId = 1L;
    private final CCAskOrderPlacementAttachment attachment = new CCAskOrderPlacementAttachment(assetId, quantity, price);

    @Mock
    OrderMatchService orderMatchService;
    @Mock
    AccountAssetService accountAssetService;
    @Mock
    AssetService assetService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;

    @InjectMocks
    CCAskOrderPlacementTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Asset asset;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_PLACEMENT, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.ASSET_ASK_ORDER_PLACEMENT, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("AskOrderPlacement", type.getName());
    }

    @Test
    void parseAttachment_fromBytes() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(25);
        buff.put((byte) 1);
        buff.putLong(assetId);
        buff.putLong(quantity);
        buff.putLong(price);
        buff.flip();

        CCAskOrderPlacementAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "AssetAskOrder tx type should be of size 25");
    }

    @Test
    void parseAttachment_fromJson() throws AplException.NotValidException {
        JSONObject json = new JSONObject();
        json.put("version.AskOrderPlacement", 1);
        json.put("asset", Long.toUnsignedString(assetId));
        json.put("quantityATU", quantity);
        json.put("priceATM", price);

        CCAskOrderPlacementAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughAsset() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity - 1);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass when not enough asset on the balance");
        verify(accountAssetService).getUnconfirmedAssetBalanceATU(senderId, assetId);
        verifyNoMoreInteractions(accountAssetService);
    }

    @Test
    void applyAttachmentUnconfirmed_negativeAssetBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(-1L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass when not asset balance is negative");
        verify(accountAssetService).getUnconfirmedAssetBalanceATU(senderId, assetId);
        verifyNoMoreInteractions(accountAssetService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass when asset balance is equal to the requested quantity");
        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_ASK_ORDER_PLACEMENT, 0, assetId, -quantity);
    }


    @Test
    void doStateDependentValidation_notEnoughAssetBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(quantity);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity - 1);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough 18446744073709551615 asset balance to place ASK order, " +
            "required: 100, but only has 99", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(quantity);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity);

        type.doStateDependentValidation(tx);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.applyAttachment(tx, sender, null);

        verify(orderMatchService).addAskOrder(tx, attachment);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_ASK_ORDER_PLACEMENT, 0, assetId, quantity);
    }
}