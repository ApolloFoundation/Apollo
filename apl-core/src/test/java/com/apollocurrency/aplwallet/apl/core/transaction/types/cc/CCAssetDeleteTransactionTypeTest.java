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
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetDeleteAttachment;
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
class CCAssetDeleteTransactionTypeTest {
    private final long assetId = -1;
    private final long quantity = 100;
    private final long senderId = 1;
    private final CCAssetDeleteAttachment attachment = new CCAssetDeleteAttachment(assetId, quantity);
    @Mock
    AssetService assetService;
    @Mock
    AccountAssetService accountAssetService;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig config;

    @InjectMocks
    CCAssetDeleteTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Asset asset;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.CC_ASSET_DELETE, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.ASSET_DELETE, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("AssetDelete", type.getName());
    }

    @Test
    void parseAttachment_fromBytes() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(17);
        buff.put((byte) 1);
        buff.putLong(assetId);
        buff.putLong(quantity);
        buff.flip();

        CCAssetDeleteAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "AssetDelete attachment must be of size 17");
    }

    @Test
    void parseAttachment_fromJson() throws AplException.NotValidException {
        JSONObject json = new JSONObject();
        json.put("version.AssetDelete", 1);
        json.put("asset", Long.toUnsignedString(assetId));
        json.put("quantityATU", quantity);

        CCAssetDeleteAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);

    }
    @Test
    void applyAttachmentUnconfirmed_negativeBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(-1L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed must not pass when account has negative asset balance");
        verify(accountAssetService).getUnconfirmedAssetBalanceATU(senderId, assetId);
        verifyNoMoreInteractions(accountAssetService);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughAssetBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity - 1);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed must not pass when account has not enough asset balance");
        verify(accountAssetService).getUnconfirmedAssetBalanceATU(senderId, assetId);
        verifyNoMoreInteractions(accountAssetService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(quantity);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass when account has enough asset balance");
        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_DELETE, 0, assetId, -quantity);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.applyAttachment(tx, sender, null);

        verify(accountAssetService).addToAssetBalanceATU(sender, LedgerEvent.ASSET_DELETE, 0L, assetId, -quantity);
        verify(assetService).deleteAsset(tx, assetId, quantity);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_DELETE, 0L, assetId, quantity);
    }

    @Test
    void doStateDependentValidation_assetNotFound() {
        when(tx.getAttachment()).thenReturn(attachment);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Asset 18446744073709551615 does not exist yet", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_assetInitialQuantityExceeded() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(99L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Invalid asset delete asset or quantity: {\"version.AssetDelete\":1," +
            "\"quantityATU\":100,\"asset\":\"18446744073709551615\"}", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_notEnoughAssetBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(2000L);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(99L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough 18446744073709551615 asset to delete: required 100," +
            " but only has 99", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(2000L);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(100L);

        type.doStateDependentValidation(tx);
    }


    @Test
    void doStateIndependentValidation_assetIdIsZero() {
        when(tx.getAttachment()).thenReturn(new CCAssetDeleteAttachment(0, quantity));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset identifier: {\"version.AssetDelete\":1,\"quantityATU\":100," +
            "\"asset\":\"0\"}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_quantityIsNegative() {
        when(tx.getAttachment()).thenReturn(new CCAssetDeleteAttachment(assetId, -1));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset quantity: -1", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(attachment);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "AssetDelete tx type must not have recipient");
    }

    @Test
    void isPhasingSafe() {
        assertTrue(type.isPhasingSafe(), "AssetDelete tx type must be phasing safe");
    }
}