/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.DigitalGoodsStore;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.ArrayList;
import java.util.List;

public class DGSService {
    private DGSPublicFeedbackTable publicFeedbackTable;
    private DGSPurchaseTable purchaseTable;
    private DGSFeedbackTable feedbackTable;
    private Blockchain blockchain;
    private DGSGoodsTable goodsTable;

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
        return purchaseTable.get(purchaseId);
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

    public DbIterator<DGSPurchase> getExpiredPendingDGSPurchases(Block block) {
        final int timestamp = block.getTimestamp();
        long privBlockId = block.getPreviousBlockId();
        Block privBlock = blockchain.getBlock(privBlockId);

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

    public List<DGSFeedback> getFeedbackNotes(DGSPurchase dgsPurchase) {
        if (!dgsPurchase.hasFeedbackNotes()) {
            return null;
        }
        dgsPurchase.setFeedbackNotes(feedbackTable.get(dgsPurchase.getDbKey()));

        return dgsPurchase.getFeedbackNotes();
    }

    private void addFeedbackNote(DGSPurchase purchase, EncryptedData feedbackNote) {
        if (purchase.getFeedbackNotes() == null) {
            purchase.setFeedbackNotes(new ArrayList<>());
        }
        purchase.getFeedbackNotes().add(feedbackNote);
        if (!purchase.hasFeedbackNotes()) {
            purchase.setHasFeedbackNotes(true);
            purchaseTable.insert(purchase);
        }
        feedbackTable.insert(this, purchase.getFeedbackNotes());
    }


    public List<DGSFeedback> getPublicFeedbacks(DGSPurchase dgsPurchase) {
        if (!dgsPurchase.hasPublicFeedbacks()) {
            return null;
        }
        dgsPurchase.setPublicFeedbacks(publicFeedbackTable.get(dgsPurchase.getId()));
        return dgsPurchase.getPublicFeedbacks();
    }

    public void addPublicFeedback(DGSPurchase dgsPurchase, String publicFeedback) {
        if (dgsPurchase.getPublicFeedbacks() == null) {
            dgsPurchase.setPublicFeedbacks(new ArrayList<>());
        }

        dgsPurchase.getPublicFeedbacks().add(publicFeedback);
        if (!dgsPurchase.hasPublicFeedbacks()) {
            dgsPurchase.setHasPublicFeedbacks(true);
            purchaseTable.insert(dgsPurchase);
        }
        publicFeedbackTable.insert(dgsPurchase, dgsPurchase.getPublicFeedbacks());
    }

    public void setDiscountATM(DGSPurchase dgsPurchase, long discountATM) {
        dgsPurchase.setDiscountATM(discountATM);
        purchaseTable.insert(dgsPurchase);
    }

    public void setRefundATM(DGSPurchase dgsPurchase, long refundATM) {
        dgsPurchase.setRefundATM(refundATM);
        purchaseTable.insert(dgsPurchase);
    }

    public void listGoods(Transaction transaction, DigitalGoodsListing attachment) {
        DGSGoods goods = new DGSGoods(transaction, attachment, blockchain.getLastBlockTimestamp());
        DigitalGoodsStore.Tag.add(goods);
        Goods.goodsTable.insert(goods);
    }

    public  void delistGoods(long goodsId) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.setDelisted(true);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    public void changePrice(long goodsId, long priceATM) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changePrice(priceATM);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    public void changeQuantity(long goodsId, int deltaQuantity) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(goodsId));
        if (! goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    public void purchase(Transaction transaction,  DigitalGoodsPurchase attachment) {
        Goods goods = Goods.goodsTable.get(Goods.goodsDbKeyFactory.newKey(attachment.getGoodsId()));
        if (! goods.isDelisted()
                && attachment.getQuantity() <= goods.getQuantity()
                && attachment.getPriceATM() == goods.getPriceATM()) {
            goods.changeQuantity(-attachment.getQuantity());
            Purchase purchase = new Purchase(transaction, attachment, goods.getSellerId());
            Purchase.purchaseTable.insert(purchase);
        } else {
            Account buyer = Account.getAccount(transaction.getSenderId());
            buyer.addToUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_DELISTED, transaction.getId(),
                    Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    public void deliver(Transaction transaction, DigitalGoodsDelivery attachment) {
        Purchase purchase = Purchase.getPendingPurchase(attachment.getPurchaseId());
        purchase.setPending(false);
        long totalWithoutDiscount = Math.multiplyExact((long) purchase.getQuantity(), purchase.getPriceATM());
        Account buyer = Account.getAccount(purchase.getBuyerId());
        long transactionId = transaction.getId();
        buyer.addToBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId,
                Math.subtractExact(attachment.getDiscountATM(), totalWithoutDiscount));
        buyer.addToUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, attachment.getDiscountATM());
        Account seller = Account.getAccount(transaction.getSenderId());
        seller.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId,
                Math.subtractExact(totalWithoutDiscount, attachment.getDiscountATM()));
        purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
        purchase.setDiscountATM(attachment.getDiscountATM());
    }

    public void refund(LedgerEvent event, long eventId, long sellerId, long purchaseId, long refundATM,
                              EncryptedMessageAppendix encryptedMessage) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
        Account seller = Account.getAccount(sellerId);
        seller.addToBalanceATM(event, eventId, -refundATM);
        Account buyer = Account.getAccount(purchase.getBuyerId());
        buyer.addToBalanceAndUnconfirmedBalanceATM(event, eventId, refundATM);
        if (encryptedMessage != null) {
            purchase.setRefundNote(encryptedMessage.getEncryptedData());
        }
        purchase.setRefundATM(refundATM);
    }

    public void feedback(long purchaseId, EncryptedMessageAppendix encryptedMessage, MessageAppendix message) {
        Purchase purchase = Purchase.purchaseTable.get(Purchase.purchaseDbKeyFactory.newKey(purchaseId));
        if (encryptedMessage != null) {
            purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
        }
        if (message != null) {
            purchase.addPublicFeedback(Convert.toString(message.getMessage()));
        }
    }
    private final DbClause inStockClause = new DbClause.BooleanClause("goods.delisted", false)
            .and(new DbClause.LongClause("goods.quantity", DbClause.Op.GT, 0));

    public  int getGoodsCount() {
        return goodsTable.getCount();
    }

    public int getCountInStock() {
        return goodsTable.getCount(inStockClause);
    }

    public DGSGoods getDGSGoods(long goodsId) {
        return goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
    }

    public DbIterator<DGSGoods> getAllDGSGoods(int from, int to) {
        return goodsTable.getAll(from, to);
    }

    public DbIterator<DGSGoods> getDGSGoodsInStock(int from, int to) {
        return goodsTable.getManyBy(inStockClause, from, to);
    }

    public DbIterator<DGSGoods> getSellerDGSGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
        return goodsTable.getManyBy(new SellerDbClause(sellerId, inStockOnly), from, to, " ORDER BY name ASC, timestamp DESC, id ASC ");
    }

    public int getSellerDGSGoodsCount(long sellerId, boolean inStockOnly) {
        return goodsTable.getCount(new SellerDbClause(sellerId, inStockOnly));
    }

    public DbIterator<DGSGoods> searchDGSGoods(String query, boolean inStockOnly, int from, int to) {
        return goodsTable.search(query, inStockOnly ? inStockClause : DbClause.EMPTY_CLAUSE, from, to,
                " ORDER BY ft.score DESC, goods.timestamp DESC ");
    }

    public DbIterator<DGSGoods> searchSellerDGSGoods(String query, long sellerId, boolean inStockOnly, int from, int to) {
        return goodsTable.search(query, new SellerDbClause(sellerId, inStockOnly), from, to,
                " ORDER BY ft.score DESC, goods.name ASC, goods.timestamp DESC ");
    }

}

