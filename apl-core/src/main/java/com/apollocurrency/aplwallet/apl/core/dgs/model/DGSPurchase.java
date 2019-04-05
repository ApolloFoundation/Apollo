/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.ArrayList;
import java.util.List;

public class DGSPurchase {

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    private final long id;
    private  DbKey dbKey; //cached value
    private final long buyerId;
    private final long goodsId;
    private final long sellerId;
    private final int quantity;
    private final long priceATM;
    private final int deadline;
    private final EncryptedData note;
    private final int timestamp;
    private boolean isPending;
    private EncryptedData encryptedGoods;
    private boolean goodsIsText;
    private EncryptedData refundNote;
    private boolean hasFeedbackNotes;
    private List<EncryptedData> feedbackNotes;
    private boolean hasPublicFeedbacks;
    List<DGSFeedback> dgsFeedbacks;
    private List<String> publicFeedbacks;
    private long discountATM;
    private long refundATM;

    private DGSPurchase(Transaction transaction, DigitalGoodsPurchase attachment, long sellerId, int lastBlockchainTimestamp) {
        this.id = transaction.getId();
        this.buyerId = transaction.getSenderId();
        this.goodsId = attachment.getGoodsId();
        this.sellerId = sellerId;
        this.quantity = attachment.getQuantity();
        this.priceATM = attachment.getPriceATM();
        this.deadline = attachment.getDeliveryDeadlineTimestamp();
        this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
        this.timestamp = lastBlockchainTimestamp;
        this.isPending = true;
    }

    public DGSPurchase(long id, long buyerId, long goodsId, long sellerId, int quantity, long priceATM, int deadline, EncryptedData note, int timestamp, boolean isPending, EncryptedData encryptedGoods, boolean goodsIsText, EncryptedData refundNote, boolean hasFeedbackNotes, List<EncryptedData> feedbackNotes, boolean hasPublicFeedbacks, List<String> publicFeedbacks, long discountATM, long refundATM) {
        this.id = id;
        this.buyerId = buyerId;
        this.goodsId = goodsId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.priceATM = priceATM;
        this.deadline = deadline;
        this.note = note;
        this.timestamp = timestamp;
        this.isPending = isPending;
        this.encryptedGoods = encryptedGoods;
        this.goodsIsText = goodsIsText;
        this.refundNote = refundNote;
        this.hasFeedbackNotes = hasFeedbackNotes;
        this.feedbackNotes = feedbackNotes;
        this.hasPublicFeedbacks = hasPublicFeedbacks;
        this.publicFeedbacks = publicFeedbacks;
        this.discountATM = discountATM;
        this.refundATM = refundATM;
    }

    public int getDeadline() {
        return deadline;
    }

    public boolean isGoodsIsText() {
        return goodsIsText;
    }

    public boolean isHasFeedbackNotes() {
        return hasFeedbackNotes;
    }

    public boolean isHasPublicFeedbacks() {
        return hasPublicFeedbacks;
    }

    public long getId() {
        return id;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public long getSellerId() { return sellerId; }

    public int getQuantity() {
        return quantity;
    }

    public long getPriceATM() {
        return priceATM;
    }

    public int getDeliveryDeadlineTimestamp() {
        return deadline;
    }

    public EncryptedData getNote() {
        return note;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean isPending) {
        this.isPending = isPending;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public EncryptedData getEncryptedGoods() {
        return encryptedGoods;
    }

    public boolean goodsIsText() {
        return goodsIsText;
    }

    public void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
        this.encryptedGoods = encryptedGoods;
        this.goodsIsText = goodsIsText;
    }

    public EncryptedData getRefundNote() {
        return refundNote;
    }

    public void setRefundNote(EncryptedData refundNote) {
        this.refundNote = refundNote;
    }

    public boolean hasFeedbackNotes() {
        return hasFeedbackNotes;
    }

    public List<EncryptedData> getFeedbackNotes() {
        if (!hasFeedbackNotes) {
            return null;
        }
//        feedbackNotes = feedbackTable.get(feedbackDbKeyFactory.newKey(this));
        return feedbackNotes;
    }

    public void addFeedbackNote(EncryptedData feedbackNote) {
        if (getFeedbackNotes() == null) {
            feedbackNotes = new ArrayList<>();
        }
        feedbackNotes.add(feedbackNote);
        if (!this.hasFeedbackNotes) {
            this.hasFeedbackNotes = true;
//            purchaseTable.insert(this);
        }
//        feedbackTable.insert(this, feedbackNotes);
    }

    public boolean hasPublicFeedbacks() {
        return hasPublicFeedbacks;
    }

    public List<String> getPublicFeedbacks() {
        if (!hasPublicFeedbacks) {
            return null;
        }
//        publicFeedbacks = publicFeedbackTable.get(publicFeedbackDbKeyFactory.newKey(this));
        return publicFeedbacks;
    }

    public void addPublicFeedback(String publicFeedback) {
        if (getPublicFeedbacks() == null) {
            publicFeedbacks = new ArrayList<>();
        }
        publicFeedbacks.add(publicFeedback);
        if (!this.hasPublicFeedbacks) {
            this.hasPublicFeedbacks = true;
//            purchaseTable.insert(this);
        }
//        publicFeedbackTable.insert(this, publicFeedbacks);
    }

    public long getDiscountATM() {
        return discountATM;
    }

    public void setDiscountATM(long discountATM) {
        this.discountATM = discountATM;
    }

    public long getRefundATM() {
        return refundATM;
    }

    public void setRefundATM(long refundATM) {
        this.refundATM = refundATM;
    }

}
