/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetDelete;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class CCAssetDelete extends ColoredCoins {
    
    public CCAssetDelete() {
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_ASSET_DELETE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_DELETE;
    }

    @Override
    public String getName() {
        return "AssetDelete";
    }

    @Override
    public ColoredCoinsAssetDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAssetDelete(buffer);
    }

    @Override
    public ColoredCoinsAssetDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAssetDelete(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        senderAccount.addToAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
        Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetDelete attachment = (ColoredCoinsAssetDelete) transaction.getAttachment();
        if (attachment.getAssetId() == 0) {
            throw new AplException.NotValidException("Invalid asset identifier: " + attachment.getJSONObject());
        }
        Asset asset = Asset.getAsset(attachment.getAssetId());
        if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
            throw new AplException.NotValidException("Invalid asset delete asset or quantity: " + attachment.getJSONObject());
        }
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
        }
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
    
}
