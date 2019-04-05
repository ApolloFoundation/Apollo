/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.ArrayList;
import java.util.List;

public class DGSService {
    private DGSPublicFeedbackTable publicFeedbackTable;
    private DGSPurchaseTable purchaseTable;
    private DGSFeedbackTable feedbackTable;
    private Blockchain blockchain;
    public  int getCount() {
        return purchaseTable.getCount();
    }

    public  int getCount(boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly));
    }

    public  DbIterator<DGSPurchase> getAllDGSPurchases(int from, int to) {
        return purchaseTable.getAll(from, to);
    }

    public  DbIterator<DGSPurchase> getDGSPurchases(boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public  DbIterator<DGSPurchase> getSellerDGSPurchases(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public  int getSellerDGSPurchaseCount(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly));
    }

    public  DbIterator<DGSPurchase> getBuyerDGSPurchases(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public  int getBuyerDGSPurchaseCount(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public  DbIterator<DGSPurchase> getSellerBuyerDGSPurchases(final long sellerId, final long buyerId,
                                                                     boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public  int getSellerBuyerDGSPurchaseCount(final long sellerId, final long buyerId,
                                                     boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public  DbIterator<DGSPurchase> getGoodsDGSPurchases(long goodsId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        DbClause clause = new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly);
        if (buyerId != 0) {
            clause = clause.and(new DbClause.LongClause("buyer_id", buyerId));
        }
        return purchaseTable.getManyBy(clause, from, to);
    }

    public  int getGoodsDGSPurchaseCount(final long goodsId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly));
    }

    public  DGSPurchase getDGSPurchase(long purchaseId) {
        return purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
    }

    public  DbIterator<DGSPurchase> getPendingSellerDGSPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId).and(new DbClause.BooleanClause("pending", true));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public  DbIterator<DGSPurchase> getExpiredSellerDGSPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId)
                .and(new DbClause.BooleanClause("pending", false))
                .and(new DbClause.NullClause("goods"));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public  DGSPurchase getPendingDGSPurchase(long purchaseId) {
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


    public void setPending(DGSPurchase purchase, boolean isPending) {
        purchase.setPending(isPending);
        purchaseTable.insert(purchase, blockchain.getHeight());
    }

    private void setEncryptedGoods(DGSPurchase purchase, EncryptedData encryptedGoods, boolean goodsIsText) {
        purchase.setEncryptedGoods(encryptedGoods, goodsIsText);
        purchaseTable.insert(purchase, blockchain.getHeight());
    }


    private void setRefundNote(DGSPurchase purchase, EncryptedData refundNote) {
        purchase.setRefundNote(refundNote);
        purchaseTable.insert(purchase, blockchain.getHeight());
    }

    public List<EncryptedData> getFeedbackNotes(DGSPurchase dgsPurchase) {
        if (!dgsPurchase.hasFeedbackNotes()) {
            return null;
        }
        List<DGSFeedback> dgsFeedbacks = feedbackTable.get(dgsPurchase.getDbKey());

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

}
