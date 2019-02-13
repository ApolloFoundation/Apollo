/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.core.app.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.app.Asset;
import com.apollocurrency.aplwallet.apl.core.app.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.app.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsAskOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsAssetDelete;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsAssetIssuance;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsBidOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsOrderCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class ColoredCoins extends TransactionType {
    
    private ColoredCoins() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_COLORED_COINS;
    }
    public static final TransactionType ASSET_ISSUANCE = new ColoredCoins() {
        private final Fee SINGLETON_ASSET_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL, 32) {
            public int getSize(TransactionImpl transaction, Appendix appendage) {
                ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
                return attachment.getDescription().length();
            }
        };
        private final Fee ASSET_ISSUANCE_FEE = (transaction, appendage) -> isSingletonIssuance(transaction) ? SINGLETON_ASSET_FEE.getFee(transaction, appendage) : 1000 * Constants.ONE_APL;

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_ISSUANCE;
        }

        @Override
        public String getName() {
            return "AssetIssuance";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return ASSET_ISSUANCE_FEE;
        }

        @Override
        public long[] getBackFees(Transaction transaction) {
            if (isSingletonIssuance(transaction)) {
                return Convert.EMPTY_LONG;
            }
            long feeATM = transaction.getFeeATM();
            return new long[]{feeATM * 3 / 10, feeATM * 2 / 10, feeATM / 10};
        }

        @Override
        public ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ColoredCoinsAssetIssuance(buffer);
        }

        @Override
        public ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ColoredCoinsAssetIssuance(attachmentData);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
            long assetId = transaction.getId();
            Asset.addAsset(transaction, attachment);
            senderAccount.addToAssetAndUnconfirmedAssetBalanceATU(getLedgerEvent(), assetId, assetId, attachment.getQuantityATU());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
            if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH || attachment.getDecimals() < 0 || attachment.getDecimals() > 8 || attachment.getQuantityATU() <= 0 || attachment.getQuantityATU() > Constants.MAX_ASSET_QUANTITY_ATU) {
                throw new AplException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
            }
            String normalizedName = attachment.getName().toLowerCase();
            for (int i = 0; i < normalizedName.length(); i++) {
                if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                    throw new AplException.NotValidException("Invalid asset name: " + normalizedName);
                }
            }
        }

        @Override
        public boolean isBlockDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
            return !isSingletonIssuance(transaction) && isDuplicate(ColoredCoins.ASSET_ISSUANCE, getName(), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }

        private boolean isSingletonIssuance(Transaction transaction) {
            ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
            return attachment.getQuantityATU() == 1 && attachment.getDecimals() == 0 && attachment.getDescription().length() <= Constants.MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH;
        }
    };
    public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_TRANSFER;
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
    };
    public static final TransactionType ASSET_DELETE = new ColoredCoins() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASSET_DELETE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_DELETE;
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
    };

    static abstract class ColoredCoinsOrderPlacement extends ColoredCoins {

        @Override
        public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ColoredCoinsOrderPlacementAttachment attachment = (ColoredCoinsOrderPlacementAttachment) transaction.getAttachment();
            if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || attachment.getAssetId() == 0) {
                throw new AplException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
            }
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (attachment.getQuantityATU() <= 0 || (asset != null && attachment.getQuantityATU() > asset.getInitialQuantityATU())) {
                throw new AplException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
            }
            if (asset == null) {
                throw new AplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) + " does not exist yet");
            }
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }
    }
    public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_ASK_ORDER_PLACEMENT;
        }

        @Override
        public String getName() {
            return "AskOrderPlacement";
        }

        @Override
        public ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ColoredCoinsAskOrderPlacement(buffer);
        }

        @Override
        public ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ColoredCoinsAskOrderPlacement(attachmentData);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
            long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
            if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
                senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
                return true;
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
            Order.Ask.addOrder(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
            senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
        }
    };
    public static final TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_BID_ORDER_PLACEMENT;
        }

        @Override
        public String getName() {
            return "BidOrderPlacement";
        }

        @Override
        public ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ColoredCoinsBidOrderPlacement(buffer);
        }

        @Override
        public ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ColoredCoinsBidOrderPlacement(attachmentData);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM())) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
                return true;
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
            Order.Bid.addOrder(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
        }
    };

    static abstract class ColoredCoinsOrderCancellation extends ColoredCoins {

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ColoredCoinsOrderCancellationAttachment attachment = (ColoredCoinsOrderCancellationAttachment) transaction.getAttachment();
            return TransactionType.isDuplicate(ColoredCoins.ASK_ORDER_CANCELLATION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }
    }
    public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_ASK_ORDER_CANCELLATION;
        }

        @Override
        public String getName() {
            return "AskOrderCancellation";
        }

        @Override
        public ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ColoredCoinsAskOrderCancellation(buffer);
        }

        @Override
        public ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ColoredCoinsAskOrderCancellation(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
            Order order = Order.Ask.getAskOrder(attachment.getOrderId());
            Order.Ask.removeOrder(attachment.getOrderId());
            if (order != null) {
                senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), order.getAssetId(), order.getQuantityATU());
            }
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
            Order ask = Order.Ask.getAskOrder(attachment.getOrderId());
            if (ask == null) {
                throw new AplException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
            }
            if (ask.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(ask.getAccountId()));
            }
        }
    };
    public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_BID_ORDER_CANCELLATION;
        }

        @Override
        public String getName() {
            return "BidOrderCancellation";
        }

        @Override
        public ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ColoredCoinsBidOrderCancellation(buffer);
        }

        @Override
        public ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ColoredCoinsBidOrderCancellation(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
            Order order = Order.Bid.getBidOrder(attachment.getOrderId());
            Order.Bid.removeOrder(attachment.getOrderId());
            if (order != null) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(order.getQuantityATU(), order.getPriceATM()));
            }
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
            Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
            if (bid == null) {
                throw new AplException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
            }
            if (bid.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(bid.getAccountId()));
            }
        }
    };
    public static final TransactionType DIVIDEND_PAYMENT = new ColoredCoins() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.ASSET_DIVIDEND_PAYMENT;
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
    };
    
}
