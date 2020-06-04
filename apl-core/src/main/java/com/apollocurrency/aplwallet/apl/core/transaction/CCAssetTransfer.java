/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
class CCAssetTransfer extends ColoredCoins {

    public CCAssetTransfer() {
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_TRANSFER;
    }

    @Override
    public String getName() {
        return "AssetTransfer";
    }

    @Override
    public ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAssetTransfer(buffer);
    }

    @Override
    public ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAssetTransfer(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        long unconfirmedAssetBalance = lookupAccountAssetService().getUnconfirmedAssetBalanceATU(senderAccount, attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            lookupAccountAssetService().addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        lookupAccountAssetService().addToAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        if (recipientAccount.getId() == GenesisImporter.CREATOR_ID) {
            lookupAssetService().deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
        } else {
            lookupAccountAssetService().addToAssetAndUnconfirmedAssetBalanceATU(recipientAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
            AssetTransfer.addAssetTransfer(transaction, attachment);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        lookupAccountAssetService().addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        if (transaction.getAmountATM() != 0 || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset transfer amount or asset: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Asset transfer to Genesis not allowed, " + "use asset delete attachment instead");
        }
        Asset asset = lookupAssetService().getAsset(attachment.getAssetId());
        if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
            throw new AplException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
        }
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }

}
