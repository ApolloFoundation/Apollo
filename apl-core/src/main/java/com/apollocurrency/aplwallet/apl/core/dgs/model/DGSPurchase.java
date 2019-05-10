/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.sql.ResultSet;
import java.util.Collections;
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
    private boolean hasPublicFeedbacks;
    private boolean hasFeedbacks;
    private List<DGSFeedback> dgsFeedbacks = Collections.emptyList();
    private List<DGSPublicFeedback> publicFeedbacks;
    private long discountATM;
    private long refundATM;
    private int height;

    public DGSPurchase(ResultSet rs, DbKey dbKey) {
        // TODO: YL implement here
        throw new RuntimeException("implement constructor from RSet");
    }

    public DGSPurchase(ResultSet rs, DbKey dbKey, DGSFeedback dgsFeedback) {
        // TODO: YL implement here
        throw new RuntimeException("implement constructor from RSet");
    }

    public DGSPurchase(Transaction transaction, DigitalGoodsPurchase attachment,
                        long sellerId, int lastBlockchainTimestamp,  List<DGSFeedback> dgsFeedbackList) {
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
        this.dgsFeedbacks = dgsFeedbackList;
    }

    public DGSPurchase(long id, long buyerId, long goodsId, long sellerId, int quantity, long priceATM, int deadline,
                       EncryptedData note, int timestamp, boolean isPending, EncryptedData encryptedGoods,
                       boolean goodsIsText, EncryptedData refundNote, boolean hasFeedbacks,
                       List<DGSFeedback> feedbacks, boolean hasPublicFeedbacks,
                       List<DGSPublicFeedback> publicFeedbacks, long discountATM, long refundATM) {
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
        this.hasFeedbacks = hasFeedbacks;
        this.dgsFeedbacks = feedbacks;
        this.hasPublicFeedbacks = hasPublicFeedbacks;
        this.publicFeedbacks = publicFeedbacks;
        this.discountATM = discountATM;
        this.refundATM = refundATM;
    }

    public List<DGSFeedback> getDgsFeedbacks() {
        return dgsFeedbacks;
    }

    public void setFeedbacks(List<DGSFeedback> dgsFeedbacks) {
        this.dgsFeedbacks = dgsFeedbacks;
    }

    public int getDeadline() {
        return deadline;
    }

    public boolean isGoodsIsText() {
        return goodsIsText;
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

    public boolean hasFeedbacks() {
        return hasFeedbacks;
    }

    public List<DGSFeedback> getFeedbacks() {
        if (!hasFeedbacks) {
            return null;
        }
//        feedbackNotes = feedbackTable.get(feedbackDbKeyFactory.newKey(this));
        return dgsFeedbacks;
    }

    public void addFeedback(DGSFeedback feedback) {
        dgsFeedbacks.add(feedback);
    }

    public boolean hasPublicFeedbacks() {
        return hasPublicFeedbacks;
    }

    public List<DGSPublicFeedback> getPublicFeedbacks() {
        if (!hasPublicFeedbacks) {
            return null;
        }
//        publicFeedbacks = publicFeedbackTable.get(publicFeedbackDbKeyFactory.newKey(this));
        return publicFeedbacks;
    }

    public void addPublicFeedback(DGSPublicFeedback publicFeedback) {
        publicFeedbacks.add(publicFeedback);
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

    public int getHeight() {
        return height;
    }

    public void setGoodsIsText(boolean goodsIsText) {
        this.goodsIsText = goodsIsText;
    }

    public void setHasFeedbacks(boolean hasFeedbackNotes) {
        this.hasFeedbacks = hasFeedbackNotes;
    }

    public void setHasPublicFeedbacks(boolean hasPublicFeedbacks) {
        this.hasPublicFeedbacks = hasPublicFeedbacks;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setEncryptedGoods(EncryptedData encryptedGoods) {
        this.encryptedGoods = encryptedGoods;
    }

    public void setPublicFeedbacks(List<DGSPublicFeedback> publicFeedbacks) {
        this.publicFeedbacks = publicFeedbacks;
    }
}
