/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetTransferAttachment;
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
class CCAssetTransferTransactionTypeTest {
    private final long assetId = -1;
    private final long quantity = 220;
    private final long senderId = 1;
    private final long recipientId = 2;

    private final CCAssetTransferAttachment attachment = new CCAssetTransferAttachment(assetId, quantity);

    @Mock
    AccountAssetService accountAssetService;
    @Mock
    AssetService assetService;
    @Mock
    AssetTransferService assetTransferService;
    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;

    @InjectMocks
    CCAssetTransferTransactionType type;


    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Account recipient;
    @Mock
    Asset asset;

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.CC_ASSET_TRANSFER, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.ASSET_TRANSFER, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("AssetTransfer", type.getName());
    }

    @Test
    void parseAttachment_fromBytes() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(17);
        buff.put((byte) 1);
        buff.putLong(assetId);
        buff.putLong(quantity);
        buff.flip();

        CCAssetTransferAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "AssetTransfer attachment must be of size 17");
    }

    @Test
    void parseAttachment_fromJson() throws AplException.NotValidException {
        JSONObject json = new JSONObject();
        json.put("version.AssetTransfer", 1);
        json.put("asset", Long.toUnsignedString(assetId));
        json.put("quantityATU", quantity);

        CCAssetTransferAttachment parsedAttachment = type.parseAttachment(json);

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
        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_TRANSFER, 0, assetId, -quantity);
    }


    @Test
    void applyAttachment_burnAsset() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(recipient.getId()).thenReturn(GenesisImporter.CREATOR_ID);

        type.applyAttachment(tx, sender, recipient);

        verify(accountAssetService).addToAssetBalanceATU(sender, LedgerEvent.ASSET_TRANSFER, 0, assetId, -quantity);
        verify(assetService).deleteAsset(tx, assetId, quantity);
    }

    @Test
    void applyAttachment_doTransfer() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(recipient.getId()).thenReturn(recipientId);

        type.applyAttachment(tx, sender, recipient);

        verify(accountAssetService).addToAssetBalanceATU(sender, LedgerEvent.ASSET_TRANSFER, 0, assetId, -quantity);
        verify(accountAssetService).addToAssetAndUnconfirmedAssetBalanceATU(recipient, LedgerEvent.ASSET_TRANSFER, 0, assetId, quantity);
        verify(assetTransferService).addAssetTransfer(tx, attachment);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountAssetService).addToUnconfirmedAssetBalanceATU(sender, LedgerEvent.ASSET_TRANSFER, 0L, assetId, quantity);
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
        when(asset.getInitialQuantityATU()).thenReturn(219L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Invalid asset transfer asset or quantity: {\"version.AssetTransfer\":1," +
            "\"quantityATU\":220,\"asset\":\"18446744073709551615\"}", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_notEnoughAssetBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(2000L);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(219L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough 18446744073709551615 asset to transfer: required 220," +
            " but only has 219", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(attachment);
        when(assetService.getAsset(assetId)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(2000L);
        when(tx.getSenderId()).thenReturn(senderId);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(senderId, assetId)).thenReturn(220L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_txAmountIsNotZero() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getAmountATM()).thenReturn(1L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset transfer amount or asset: {\"version.AssetTransfer\":1,\"quantityATU\":220," +
            "\"asset\":\"18446744073709551615\"}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroAssetId() {
        when(tx.getAttachment()).thenReturn(new CCAssetTransferAttachment(0, quantity));
        when(tx.getAmountATM()).thenReturn(0L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset transfer amount or asset: {\"version.AssetTransfer\":1,\"quantityATU\":220," +
            "\"asset\":\"0\"}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_transferToGenesis() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getAmountATM()).thenReturn(0L);
        when(tx.getRecipientId()).thenReturn(GenesisImporter.CREATOR_ID);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Asset transfer to Genesis not allowed, use asset delete attachment instead", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroQuantity() {
        when(tx.getAttachment()).thenReturn(new CCAssetTransferAttachment(assetId, 0));
        when(tx.getAmountATM()).thenReturn(0L);
        when(tx.getRecipientId()).thenReturn(recipientId);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () ->
            type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset quantity: 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_OK() throws AplException.ValidationException {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getAmountATM()).thenReturn(0L);
        when(tx.getRecipientId()).thenReturn(recipientId);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void canHaveRecipient() {
        assertTrue(type.canHaveRecipient(), "AssetTransfer should have a recipient");
    }

    @Test
    void isPhasingSafe() {
        assertTrue(type.isPhasingSafe(), "AssetTransfer tx should be phasing safe");
    }
}