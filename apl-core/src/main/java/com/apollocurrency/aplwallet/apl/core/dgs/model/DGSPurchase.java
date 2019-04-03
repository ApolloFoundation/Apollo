/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.model;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DGSPurchase {



    private static class DGSPurchasesClause extends DbClause {

        private DGSPurchasesClause(String clause, boolean withPublicFeedbacksOnly, boolean completedOnly) {
            super(clause + (completedOnly ? " AND goods IS NOT NULL " : " ")
                    + (withPublicFeedbacksOnly ? " AND has_public_feedbacks = TRUE " : " "));
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            return index;
        }

    }

    private static final class LongDGSPurchasesClause extends DGSPurchasesClause {

        private final long value;

        private LongDGSPurchasesClause(String columnName, long value, boolean withPublicFeedbacksOnly, boolean completedOnly) {
            super(columnName + " = ? ", withPublicFeedbacksOnly, completedOnly);
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index++, value);
            return index;
        }

    }

    private static final class SellerBuyerDGSPurchasesClause extends DGSPurchasesClause {

        private final long sellerId;
        private final long buyerId;

        private SellerBuyerDGSPurchasesClause(long sellerId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
            super(" seller_id = ? AND buyer_id = ? ", withPublicFeedbacksOnly, completedOnly);
            this.sellerId = sellerId;
            this.buyerId = buyerId;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index++, sellerId);
            pstmt.setLong(index++, buyerId);
            return index;
        }

    }


    public DbKey getDbKey() {
        return dbKey;
    }

    public static int getCount() {
        return purchaseTable.getCount();
    }

    public static int getCount(boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly));
    }

    public static DbIterator<DGSPurchase> getAllDGSPurchases(int from, int to) {
        return purchaseTable.getAll(from, to);
    }

    public static DbIterator<DGSPurchase> getDGSPurchases(boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public static DbIterator<DGSPurchase> getSellerDGSPurchases(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public static int getSellerDGSPurchaseCount(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly));
    }

    public static DbIterator<DGSPurchase> getBuyerDGSPurchases(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public static int getBuyerDGSPurchaseCount(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public static DbIterator<DGSPurchase> getSellerBuyerDGSPurchases(final long sellerId, final long buyerId,
                                                               boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public static int getSellerBuyerDGSPurchaseCount(final long sellerId, final long buyerId,
                                                  boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public static DbIterator<DGSPurchase> getGoodsDGSPurchases(long goodsId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        DbClause clause = new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly);
        if (buyerId != 0) {
            clause = clause.and(new DbClause.LongClause("buyer_id", buyerId));
        }
        return purchaseTable.getManyBy(clause, from, to);
    }

    public static int getGoodsDGSPurchaseCount(final long goodsId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly));
    }

    public static DGSPurchase getDGSPurchase(long purchaseId) {
        return purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
    }

    public static DbIterator<DGSPurchase> getPendingSellerDGSPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId).and(new DbClause.BooleanClause("pending", true));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public static DbIterator<DGSPurchase> getExpiredSellerDGSPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId)
                .and(new DbClause.BooleanClause("pending", false))
                .and(new DbClause.NullClause("goods"));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public static DGSPurchase getPendingDGSPurchase(long purchaseId) {
        DGSPurchase purchase = getDGSPurchase(purchaseId);
        return purchase == null || !purchase.isPending() ? null : purchase;
    }

    public static DbIterator<DGSPurchase> getExpiredPendingDGSPurchases(Block block) {
        final int timestamp = block.getTimestamp();
        Blockchain bc = blockchain;
        long privBlockId = block.getPreviousBlockId();
        Block privBlock = bc.getBlock(privBlockId);

        final int previousTimestamp = privBlock.getTimestamp();

        DbClause dbClause = new DbClause.LongClause("deadline", DbClause.Op.LT, timestamp)
                .and(new DbClause.LongClause("deadline", DbClause.Op.GTE, previousTimestamp))
                .and(new DbClause.BooleanClause("pending", true));
        return purchaseTable.getManyBy(dbClause, 0, -1);
    }

    private static void init() {}

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
    private List<String> publicFeedbacks;
    private long discountATM;
    private long refundATM;

    private DGSPurchase(Transaction transaction, DigitalGoodsPurchase attachment, long sellerId) {
        this.id = transaction.getId();
        this.dbKey = purchaseDbKeyFactory.newKey(this.id);
        this.buyerId = transaction.getSenderId();
        this.goodsId = attachment.getGoodsId();
        this.sellerId = sellerId;
        this.quantity = attachment.getQuantity();
        this.priceATM = attachment.getPriceATM();
        this.deadline = attachment.getDeliveryDeadlineTimestamp();
        this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
        this.timestamp = blockchain.getLastBlockTimestamp();
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

    public static LongKeyFactory<DGSPurchase> getPublicFeedbackDbKeyFactory() {
        return publicFeedbackDbKeyFactory;
    }

    public static VersionedValuesDbTable<DGSPurchase, String> getPublicFeedbackTable() {
        return publicFeedbackTable;
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
        purchaseTable.insert(this);
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

    private void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
        this.encryptedGoods = encryptedGoods;
        this.goodsIsText = goodsIsText;
        purchaseTable.insert(this);
    }

    public EncryptedData getRefundNote() {
        return refundNote;
    }

    private void setRefundNote(EncryptedData refundNote) {
        this.refundNote = refundNote;
        purchaseTable.insert(this);
    }

    public boolean hasFeedbackNotes() {
        return hasFeedbackNotes;
    }

    public List<EncryptedData> getFeedbackNotes() {
        if (!hasFeedbackNotes) {
            return null;
        }
        feedbackNotes = feedbackTable.get(feedbackDbKeyFactory.newKey(this));
        return feedbackNotes;
    }

    private void addFeedbackNote(EncryptedData feedbackNote) {
        if (getFeedbackNotes() == null) {
            feedbackNotes = new ArrayList<>();
        }
        feedbackNotes.add(feedbackNote);
        if (!this.hasFeedbackNotes) {
            this.hasFeedbackNotes = true;
            purchaseTable.insert(this);
        }
        feedbackTable.insert(this, feedbackNotes);
    }

    public boolean hasPublicFeedbacks() {
        return hasPublicFeedbacks;
    }

    public List<String> getPublicFeedbacks() {
        if (!hasPublicFeedbacks) {
            return null;
        }
        publicFeedbacks = publicFeedbackTable.get(publicFeedbackDbKeyFactory.newKey(this));
        return publicFeedbacks;
    }

    private void addPublicFeedback(String publicFeedback) {
        if (getPublicFeedbacks() == null) {
            publicFeedbacks = new ArrayList<>();
        }
        publicFeedbacks.add(publicFeedback);
        if (!this.hasPublicFeedbacks) {
            this.hasPublicFeedbacks = true;
            purchaseTable.insert(this);
        }
        publicFeedbackTable.insert(this, publicFeedbacks);
    }

    public long getDiscountATM() {
        return discountATM;
    }

    private void setDiscountATM(long discountATM) {
        this.discountATM = discountATM;
        purchaseTable.insert(this);
    }

    public long getRefundATM() {
        return refundATM;
    }

    private void setRefundATM(long refundATM) {
        this.refundATM = refundATM;
        purchaseTable.insert(this);
    }

}
