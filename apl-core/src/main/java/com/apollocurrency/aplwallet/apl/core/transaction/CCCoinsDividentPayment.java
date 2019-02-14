/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class CCCoinsDividentPayment extends ColoredCoins {
    
    public CCCoinsDividentPayment() {
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_DIVIDEND_PAYMENT;
    }

    @Override
    public String getName() {
        return "DividendPayment";
    }

    @Override
    public ColoredCoinsDividendPayment parseAttachment(ByteBuffer buffer) {
        return new ColoredCoinsDividendPayment(buffer);
    }

    @Override
    public ColoredCoinsDividendPayment parseAttachment(JSONObject attachmentData) {
        return new ColoredCoinsDividendPayment(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsDividendPayment attachment = (ColoredCoinsDividendPayment) transaction.getAttachment();
        long assetId = attachment.getAssetId();
        Asset asset = Asset.getAsset(assetId, attachment.getHeight());
        if (asset == null) {
            return true;
        }
        long quantityATU = asset.getQuantityATU() - senderAccount.getAssetBalanceATU(assetId, attachment.getHeight());
        long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
        if (senderAccount.getUnconfirmedBalanceATM() >= totalDividendPayment) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -totalDividendPayment);
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsDividendPayment attachment = (ColoredCoinsDividendPayment) transaction.getAttachment();
        senderAccount.payDividends(transaction.getId(), attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsDividendPayment attachment = (ColoredCoinsDividendPayment) transaction.getAttachment();
        long assetId = attachment.getAssetId();
        Asset asset = Asset.getAsset(assetId, attachment.getHeight());
        if (asset == null) {
            return;
        }
        long quantityATU = asset.getQuantityATU() - senderAccount.getAssetBalanceATU(assetId, attachment.getHeight());
        long totalDividendPayment = Math.multiplyExact(attachment.getAmountATMPerATU(), quantityATU);
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), totalDividendPayment);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsDividendPayment attachment = (ColoredCoinsDividendPayment) transaction.getAttachment();
        if (attachment.getHeight() > blockchain.getHeight()) {
            throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight() + ", must not exceed current blockchain height " + blockchain.getHeight());
        }
        if (attachment.getHeight() <= attachment.getFinishValidationHeight(transaction) - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK) {
            throw new AplException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight() + ", must be less than " + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK + " blocks before " + attachment.getFinishValidationHeight(transaction));
        }
        Asset asset = Asset.getAsset(attachment.getAssetId(), attachment.getHeight());
        if (asset == null) {
            throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " for dividend payment doesn't exist yet");
        }
        if (asset.getAccountId() != transaction.getSenderId() || attachment.getAmountATMPerATU() <= 0) {
            throw new AplException.NotValidException("Invalid dividend payment sender or amount " + attachment.getJSONObject());
        }
        AssetDividend lastDividend = AssetDividend.getLastDividend(attachment.getAssetId());
        if (lastDividend != null && lastDividend.getHeight() > blockchain.getHeight() - 60) {
            throw new AplException.NotCurrentlyValidException("Last dividend payment for asset " + Long.toUnsignedString(attachment.getAssetId()) + " was less than 60 blocks ago at " + lastDividend.getHeight() + ", current height is " + blockchain.getHeight() + ", limit is one dividend per 60 blocks");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        ColoredCoinsDividendPayment attachment = (ColoredCoinsDividendPayment) transaction.getAttachment();
        return isDuplicate(ColoredCoins.DIVIDEND_PAYMENT, Long.toUnsignedString(attachment.getAssetId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
    
}
