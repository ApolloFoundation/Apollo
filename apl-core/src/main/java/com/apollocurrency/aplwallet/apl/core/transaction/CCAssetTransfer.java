/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        senderAccount.addToAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        if (recipientAccount.getId() == Genesis.CREATOR_ID) {
            Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
        } else {
            recipientAccount.addToAssetAndUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
            AssetTransfer.addAssetTransfer(transaction, attachment);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetTransfer attachment = (ColoredCoinsAssetTransfer) transaction.getAttachment();
        if (transaction.getAmountATM() != 0 || attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset transfer amount or asset: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
            throw new AplException.NotValidException("Asset transfer to Genesis not allowed, " + "use asset delete attachment instead");
        }
        Asset asset = Asset.getAsset(attachment.getAssetId());
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
