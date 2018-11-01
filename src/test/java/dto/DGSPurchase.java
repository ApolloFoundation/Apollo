/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.List;
import java.util.Objects;

public class DGSPurchase {
    private long id;
    private long buyerId;
    private long goodsId;
    private long sellerId;
    private int quantity;
    private long priceATM;
    private int deadline;
    private int timestamp;
    private boolean isPending;
    private boolean goodsIsText;
    private boolean hasFeedbackNotes;
    private boolean hasPublicFeedbacks;
    private List<String> publicFeedbacks;
    private long discountATM;
    private long refundATM;

    public DGSPurchase() {
    }

    @Override
    public String toString() {
        return "DGSPurchase{" +
                "id=" + id +
                ", buyerId=" + buyerId +
                ", goodsId=" + goodsId +
                ", sellerId=" + sellerId +
                ", quantity=" + quantity +
                ", priceATM=" + priceATM +
                ", deadline=" + deadline +
                ", timestamp=" + timestamp +
                ", isPending=" + isPending +
                ", goodsIsText=" + goodsIsText +
                ", hasFeedbackNotes=" + hasFeedbackNotes +
                ", hasPublicFeedbacks=" + hasPublicFeedbacks +
                ", publicFeedbacks=" + publicFeedbacks +
                ", discountATM=" + discountATM +
                ", refundATM=" + refundATM +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DGSPurchase)) return false;
        DGSPurchase that = (DGSPurchase) o;
        return id == that.id &&
                buyerId == that.buyerId &&
                goodsId == that.goodsId &&
                sellerId == that.sellerId &&
                quantity == that.quantity &&
                priceATM == that.priceATM &&
                deadline == that.deadline &&
                timestamp == that.timestamp &&
                isPending == that.isPending &&
                goodsIsText == that.goodsIsText &&
                hasFeedbackNotes == that.hasFeedbackNotes &&
                hasPublicFeedbacks == that.hasPublicFeedbacks &&
                discountATM == that.discountATM &&
                refundATM == that.refundATM &&
                Objects.equals(publicFeedbacks, that.publicFeedbacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, buyerId, goodsId, sellerId, quantity, priceATM, deadline, timestamp, isPending, goodsIsText, hasFeedbackNotes, hasPublicFeedbacks, publicFeedbacks, discountATM, refundATM);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(long buyerId) {
        this.buyerId = buyerId;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }

    public long getSellerId() {
        return sellerId;
    }

    public void setSellerId(long sellerId) {
        this.sellerId = sellerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getPriceATM() {
        return priceATM;
    }

    public void setPriceATM(long priceATM) {
        this.priceATM = priceATM;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public boolean isGoodsIsText() {
        return goodsIsText;
    }

    public void setGoodsIsText(boolean goodsIsText) {
        this.goodsIsText = goodsIsText;
    }

    public boolean isHasFeedbackNotes() {
        return hasFeedbackNotes;
    }

    public void setHasFeedbackNotes(boolean hasFeedbackNotes) {
        this.hasFeedbackNotes = hasFeedbackNotes;
    }

    public boolean isHasPublicFeedbacks() {
        return hasPublicFeedbacks;
    }

    public void setHasPublicFeedbacks(boolean hasPublicFeedbacks) {
        this.hasPublicFeedbacks = hasPublicFeedbacks;
    }

    public List<String> getPublicFeedbacks() {
        return publicFeedbacks;
    }

    public void setPublicFeedbacks(List<String> publicFeedbacks) {
        this.publicFeedbacks = publicFeedbacks;
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

