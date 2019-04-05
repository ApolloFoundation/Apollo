/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelisting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsFeedback;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPriceChange;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsQuantityChange;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsRefund;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UnencryptedDigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class DigitalGoods extends TransactionType {
    
    private static final Logger LOG = getLogger(DigitalGoods.class); 
    
    private DigitalGoods() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_DIGITAL_GOODS;
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Invalid digital goods transaction");
        }
        doValidateAttachment(transaction);
    }

    public abstract void doValidateAttachment(Transaction transaction) throws AplException.ValidationException;
    public static final TransactionType LISTING = new DigitalGoods() {
        private final Fee DGS_LISTING_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                DigitalGoodsListing attachment = (DigitalGoodsListing) transaction.getAttachment();
                return attachment.getName().length() + attachment.getDescription().length();
            }
        };

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_LISTING;
        }

        @Override
        public String getName() {
            return "DigitalGoodsListing";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return DGS_LISTING_FEE;
        }

        @Override
        public DigitalGoodsListing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsListing(buffer);
        }

        @Override
        public DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsListing(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsListing attachment = (DigitalGoodsListing) transaction.getAttachment();
            DigitalGoodsStore.listGoods(transaction, attachment);
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsListing attachment = (DigitalGoodsListing) transaction.getAttachment();
            if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY || attachment.getPriceATM() <= 0 || attachment.getPriceATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
            }
            PrunablePlainMessageAppendix prunablePlainMessage = transaction.getPrunablePlainMessage();
            if (prunablePlainMessage != null) {
                byte[] image = prunablePlainMessage.getMessage();
                if (image != null) {
                    Tika tika = new Tika();
                    MediaType mediaType = null;
                    try {
                        String mediaTypeName = tika.detect(image);
                        mediaType = MediaType.parse(mediaTypeName);
                    } catch (NoClassDefFoundError e) {
                        LOG.error("Error running Tika parsers", e);
                    }
                    if (mediaType == null || !"image".equals(mediaType.getType())) {
                        throw new AplException.NotValidException("Only image attachments allowed for DGS listing, media type is " + mediaType);
                    }
                }
            }
        }

        @Override
        public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return isDuplicate(DigitalGoods.LISTING, getName(), duplicates, true);
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
    public static final TransactionType DELISTING = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_DELISTING;
        }

        @Override
        public String getName() {
            return "DigitalGoodsDelisting";
        }

        @Override
        public DigitalGoodsDelisting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsDelisting(buffer);
        }

        @Override
        public DigitalGoodsDelisting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsDelisting(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsDelisting attachment = (DigitalGoodsDelisting) transaction.getAttachment();
            DigitalGoodsStore.delistGoods(attachment.getGoodsId());
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsDelisting attachment = (DigitalGoodsDelisting) transaction.getAttachment();
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
            if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
                throw new AplException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
            }
            if (goods == null || goods.isDelisted()) {
                throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsDelisting attachment = (DigitalGoodsDelisting) transaction.getAttachment();
            return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
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
    public static final TransactionType PRICE_CHANGE = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_PRICE_CHANGE;
        }

        @Override
        public String getName() {
            return "DigitalGoodsPriceChange";
        }

        @Override
        public DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsPriceChange(buffer);
        }

        @Override
        public DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsPriceChange(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsPriceChange attachment = (DigitalGoodsPriceChange) transaction.getAttachment();
            DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceATM());
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsPriceChange attachment = (DigitalGoodsPriceChange) transaction.getAttachment();
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
            if (attachment.getPriceATM() <= 0 || attachment.getPriceATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                throw new AplException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
            }
            if (goods == null || goods.isDelisted()) {
                throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsPriceChange attachment = (DigitalGoodsPriceChange) transaction.getAttachment();
            // not a bug, uniqueness is based on DigitalGoods.DELISTING
            return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
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
    public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_QUANTITY_CHANGE;
        }

        @Override
        public String getName() {
            return "DigitalGoodsQuantityChange";
        }

        @Override
        public DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsQuantityChange(buffer);
        }

        @Override
        public DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsQuantityChange(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
            DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
            if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                throw new AplException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
            }
            if (goods == null || goods.isDelisted()) {
                throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsQuantityChange attachment = (DigitalGoodsQuantityChange) transaction.getAttachment();
            // not a bug, uniqueness is based on DigitalGoods.DELISTING
            return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
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
    public static final TransactionType PURCHASE = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_PURCHASE;
        }

        @Override
        public String getName() {
            return "DigitalGoodsPurchase";
        }

        @Override
        public DigitalGoodsPurchase parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsPurchase(buffer);
        }

        @Override
        public DigitalGoodsPurchase parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsPurchase(attachmentData);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DigitalGoodsPurchase attachment = (DigitalGoodsPurchase) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM())) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
                return true;
            }
            return false;
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DigitalGoodsPurchase attachment = (DigitalGoodsPurchase) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsPurchase attachment = (DigitalGoodsPurchase) transaction.getAttachment();
            DigitalGoodsStore.purchase(transaction, attachment);
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsPurchase attachment = (DigitalGoodsPurchase) transaction.getAttachment();
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
            if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY || attachment.getPriceATM() <= 0 || attachment.getPriceATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || (goods != null && goods.getSellerId() != transaction.getRecipientId())) {
                throw new AplException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
            }
            if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                throw new AplException.NotValidException("Only text encrypted messages allowed");
            }
            if (goods == null || goods.isDelisted()) {
                throw new AplException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) + "not yet listed or already delisted");
            }
            if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceATM() != goods.getPriceATM()) {
                throw new AplException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
            }
            if (attachment.getDeliveryDeadlineTimestamp() <= blockchain.getLastBlockTimestamp()) {
                throw new AplException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsPurchase attachment = (DigitalGoodsPurchase) transaction.getAttachment();
            // not a bug, uniqueness is based on DigitalGoods.DELISTING
            return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, false);
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType DELIVERY = new DigitalGoods() {
        private final Fee DGS_DELIVERY_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
                return attachment.getGoodsDataLength() - 16;
            }
        };

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_DELIVERY;
        }

        @Override
        public String getName() {
            return "DigitalGoodsDelivery";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return DGS_DELIVERY_FEE;
        }

        @Override
        public DigitalGoodsDelivery parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsDelivery(buffer);
        }

        @Override
        public DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            if (attachmentData.get("goodsData") == null) {
                return new UnencryptedDigitalGoodsDelivery(attachmentData);
            }
            return new DigitalGoodsDelivery(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
            DigitalGoodsStore.deliver(transaction, attachment);
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPendingPurchase(attachment.getPurchaseId());
            if (attachment.getGoodsDataLength() > Constants.MAX_DGS_GOODS_LENGTH) {
                throw new AplException.NotValidException("Invalid digital goods delivery data length: " + attachment.getGoodsDataLength());
            }
            if (attachment.getGoods() != null) {
                if (attachment.getGoods().getData().length == 0 || attachment.getGoods().getNonce().length != 32) {
                    throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
            }
            if (attachment.getDiscountATM() < 0 || attachment.getDiscountATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || (purchase != null && (purchase.getBuyerId() != transaction.getRecipientId() || transaction.getSenderId() != purchase.getSellerId() || attachment.getDiscountATM() > Math.multiplyExact(purchase.getPriceATM(), (long) purchase.getQuantity())))) {
                throw new AplException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
            }
            if (purchase == null || purchase.getEncryptedGoods() != null) {
                throw new AplException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: " + attachment.getJSONObject());
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsDelivery attachment = (DigitalGoodsDelivery) transaction.getAttachment();
            return isDuplicate(DigitalGoods.DELIVERY, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType FEEDBACK = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_FEEDBACK;
        }

        @Override
        public String getName() {
            return "DigitalGoodsFeedback";
        }

        @Override
        public DigitalGoodsFeedback parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsFeedback(buffer);
        }

        @Override
        public DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsFeedback(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsFeedback attachment = (DigitalGoodsFeedback) transaction.getAttachment();
            DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsFeedback attachment = (DigitalGoodsFeedback) transaction.getAttachment();
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
            if (purchase != null && (purchase.getSellerId() != transaction.getRecipientId() || transaction.getSenderId() != purchase.getBuyerId())) {
                throw new AplException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
            }
            if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                throw new AplException.NotValidException("Missing feedback message");
            }
            if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                throw new AplException.NotValidException("Only text encrypted messages allowed");
            }
            if (transaction.getMessage() != null && !transaction.getMessage().isText()) {
                throw new AplException.NotValidException("Only text public messages allowed");
            }
            if (purchase == null || purchase.getEncryptedGoods() == null) {
                throw new AplException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType REFUND = new DigitalGoods() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.DIGITAL_GOODS_REFUND;
        }

        @Override
        public String getName() {
            return "DigitalGoodsRefund";
        }

        @Override
        public DigitalGoodsRefund parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new DigitalGoodsRefund(buffer);
        }

        @Override
        public DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new DigitalGoodsRefund(attachmentData);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getRefundATM()) {
                senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -attachment.getRefundATM());
                return true;
            }
            return false;
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), attachment.getRefundATM());
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
            DigitalGoodsStore.refund(getLedgerEvent(), transaction.getId(), transaction.getSenderId(), attachment.getPurchaseId(), attachment.getRefundATM(), transaction.getEncryptedMessage());
        }

        @Override
        public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
            DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
            DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
            if (attachment.getRefundATM() < 0 || attachment.getRefundATM() > blockchainConfig.getCurrentConfig().getMaxBalanceATM() || (purchase != null && (purchase.getBuyerId() != transaction.getRecipientId() || transaction.getSenderId() != purchase.getSellerId()))) {
                throw new AplException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
            }
            if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                throw new AplException.NotValidException("Only text encrypted messages allowed");
            }
            if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundATM() != 0) {
                throw new AplException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
            return isDuplicate(DigitalGoods.REFUND, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    
}
