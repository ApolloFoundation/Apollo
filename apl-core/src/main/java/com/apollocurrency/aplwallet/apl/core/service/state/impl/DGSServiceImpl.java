/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.dbclause.LongDGSPurchasesClause;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.dbclause.SellerBuyerDGSPurchasesClause;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.dbclause.SellerDbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.DGSPurchasesClause;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DGSServiceImpl implements DGSService {
    private final DbClause inStockOnlyClause = new DbClause.IntClause("in_stock_count", DbClause.Op.GT, 0);
    private final DbClause inStockClause = new DbClause.BooleanClause("goods.delisted", false)
        .and(new DbClause.LongClause("goods.quantity", DbClause.Op.GT, 0));
    private DGSPublicFeedbackTable publicFeedbackTable;
    private DGSPurchaseTable purchaseTable;
    private DGSFeedbackTable feedbackTable;
    private Blockchain blockchain;
    private DGSGoodsTable goodsTable;
    private DGSTagTable tagTable;
    private AccountService accountService;

    @Inject
    public DGSServiceImpl(DGSPublicFeedbackTable publicFeedbackTable, DGSPurchaseTable purchaseTable, DGSFeedbackTable feedbackTable, Blockchain blockchain, DGSGoodsTable goodsTable, DGSTagTable tagTable, AccountService accountService) {
        this.publicFeedbackTable = publicFeedbackTable;
        this.purchaseTable = purchaseTable;
        this.feedbackTable = feedbackTable;
        this.blockchain = blockchain;
        this.goodsTable = goodsTable;
        this.tagTable = tagTable;
        this.accountService = accountService;
    }

    public int getPurchaseCount() {
        return purchaseTable.getCount();
    }

    public int getPurchaseCount(boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly));
    }

    public DbIterator<DGSPurchase> getAllPurchases(int from, int to) {
        return purchaseTable.getAll(from, to);
    }

    public DbIterator<DGSPurchase> getPurchases(boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new DGSPurchasesClause(" TRUE ", withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public DbIterator<DGSPurchase> getSellerPurchases(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public int getSellerPurchaseCount(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("seller_id", sellerId, withPublicFeedbacksOnly, completedOnly));
    }

    public DbIterator<DGSPurchase> getBuyerPurchases(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public int getBuyerPurchaseCount(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("buyer_id", buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public DbIterator<DGSPurchase> getSellerBuyerPurchases(final long sellerId, final long buyerId,
                                                           boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        return purchaseTable.getManyBy(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly), from, to);
    }

    public int getSellerBuyerPurchaseCount(final long sellerId, final long buyerId,
                                           boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new SellerBuyerDGSPurchasesClause(sellerId, buyerId, withPublicFeedbacksOnly, completedOnly));
    }

    public DbIterator<DGSPurchase> getGoodsPurchases(long goodsId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to) {
        DbClause clause = new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly);
        if (buyerId != 0) {
            clause = clause.and(new DbClause.LongClause("buyer_id", buyerId));
        }
        return purchaseTable.getManyBy(clause, from, to);
    }

    public int getGoodsPurchaseCount(final long goodsId, boolean withPublicFeedbacksOnly, boolean completedOnly) {
        return purchaseTable.getCount(new LongDGSPurchasesClause("goods_id", goodsId, withPublicFeedbacksOnly, completedOnly));
    }

    public DGSPurchase getPurchase(long purchaseId) {
        return purchaseTable.get(purchaseId);
    }

    public DbIterator<DGSPurchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId).and(new DbClause.BooleanClause("pending", true));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public DbIterator<DGSPurchase> getExpiredSellerPurchases(final long sellerId, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("seller_id", sellerId)
            .and(new DbClause.BooleanClause("pending", false))
            .and(new DbClause.NullClause("goods"));
        return purchaseTable.getManyBy(dbClause, from, to);
    }

    public DGSPurchase getPendingPurchase(long purchaseId) {
        DGSPurchase purchase = getPurchase(purchaseId);
        return purchase == null || !purchase.isPending() ? null : purchase;
    }

    public DbIterator<DGSPurchase> getExpiredPendingPurchases(Block block) {
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
        purchaseTable.insert(purchase/*, blockchain.getHeight()*/);
    }

    private void setEncryptedGoods(DGSPurchase purchase, EncryptedData encryptedGoods, boolean goodsIsText) {
        purchase.setEncryptedGoods(encryptedGoods, goodsIsText);
        purchaseTable.insert(purchase/*, blockchain.getHeight()*/);
    }

    private void setRefundNote(DGSPurchase purchase, EncryptedData refundNote) {
        purchase.setRefundNote(refundNote);
        purchaseTable.insert(purchase/*, blockchain.getHeight()*/);
    }

    public List<DGSFeedback> getFeedbacks(DGSPurchase dgsPurchase) {
        if (!dgsPurchase.hasFeedbacks()) {
            return null;
        }
        dgsPurchase.setFeedbacks(feedbackTable.get(dgsPurchase.getId()));

        return dgsPurchase.getFeedbacks();
    }

    private void addFeedback(DGSPurchase purchase, EncryptedData feedbackNote) {
        if (getFeedbacks(purchase) == null) {
            purchase.setFeedbacks(new ArrayList<>());
        }
        int blockchainHeight = blockchain.getHeight();
        if (!purchase.hasFeedbacks()) {
            purchase.setHasFeedbacks(true);
            purchase.setHeight(blockchainHeight);
            purchaseTable.insert(purchase);
        }
        DGSFeedback feedback = new DGSFeedback(null, blockchainHeight, purchase.getId(), feedbackNote);
        purchase.addFeedback(feedback);
        purchase.getFeedbacks().forEach((f -> f.setHeight(blockchainHeight)));

        feedbackTable.insert(purchase.getFeedbacks());
    }

    private void addPublicFeedback(DGSPurchase dgsPurchase, String publicFeedback) {

        if (getPublicFeedbacks(dgsPurchase) == null) {
            dgsPurchase.setPublicFeedbacks(new ArrayList<>());
        }
        int blockchainHeight = blockchain.getHeight();
        if (!dgsPurchase.hasPublicFeedbacks()) {
            dgsPurchase.setHasPublicFeedbacks(true);
            dgsPurchase.setHeight(blockchainHeight);
            purchaseTable.insert(dgsPurchase);
        }
        DGSPublicFeedback dgsPublicFeedback = new DGSPublicFeedback(null, blockchainHeight, publicFeedback, dgsPurchase.getId());
        dgsPurchase.getPublicFeedbacks().add(dgsPublicFeedback);
        dgsPurchase.getPublicFeedbacks().forEach((f -> f.setHeight(blockchainHeight)));
        publicFeedbackTable.insert(dgsPurchase.getPublicFeedbacks());
    }

    public List<DGSPublicFeedback> getPublicFeedbacks(DGSPurchase dgsPurchase) {
        if (!dgsPurchase.hasPublicFeedbacks()) {
            return null;
        }
        dgsPurchase.setPublicFeedbacks(publicFeedbackTable.get(dgsPurchase.getId()));
        return dgsPurchase.getPublicFeedbacks();
    }

    private void setDiscountATM(DGSPurchase dgsPurchase, long discountATM) {
        dgsPurchase.setDiscountATM(discountATM);
        purchaseTable.insert(dgsPurchase);
    }

    private void setRefundATM(DGSPurchase dgsPurchase, long refundATM) {
        dgsPurchase.setRefundATM(refundATM);
        purchaseTable.insert(dgsPurchase);
    }

    public void listGoods(Transaction transaction, DigitalGoodsListing attachment) {
        DGSGoods goods = new DGSGoods(transaction, attachment, blockchain.getLastBlockTimestamp());
        addTag(goods);
        goodsTable.insert(goods);
    }

    public int getTagsCount() {
        return tagTable.getCount();
    }

    public int getCountInStock() {
        return tagTable.getCount(inStockOnlyClause);
    }

    public DbIterator<DGSTag> getAllTags(int from, int to) {
        return tagTable.getAll(from, to);
    }

    public DbIterator<DGSTag> getInStockTags(int from, int to) {
        return tagTable.getManyBy(inStockOnlyClause, from, to);
    }

    public DbIterator<DGSTag> getTagsLike(String prefix, boolean inStockOnly, int from, int to) {
        DbClause dbClause = new DbClause.LikeClause("tag", prefix);
        if (inStockOnly) {
            dbClause = dbClause.and(inStockOnlyClause);
        }
        return tagTable.getManyBy(dbClause, from, to, " ORDER BY tag ");
    }

    private void addTag(DGSGoods goods) {
        for (String tagValue : goods.getParsedTags()) {
            DGSTag tag = tagTable.get(tagValue);
            if (tag == null) {
                tag = new DGSTag(tagValue, blockchain.getHeight());
            }
            tag.setInStockCount(tag.getInStockCount() + 1);
            tag.setTotalCount(tag.getTotalCount() + 1);
            tag.setHeight(blockchain.getHeight());
            tagTable.insert(tag);
        }
    }

    private void delistTag(DGSGoods goods) {
        for (String tagValue : goods.getParsedTags()) {
            DGSTag tag = tagTable.get(tagValue);
            if (tag == null) {
                throw new IllegalStateException("Unknown tag " + tagValue);
            }
            tag.setInStockCount(tag.getInStockCount() - 1);
            tag.setHeight(blockchain.getHeight());
            tagTable.insert(tag);
        }
    }

    public void delistGoods(long goodsId) {
        DGSGoods goods = goodsTable.get(goodsId);
        if (!goods.isDelisted()) {
            goods.setHeight(blockchain.getHeight());
            setDelistedGoods(goods, true);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    public void changePrice(long goodsId, long priceATM) {
        DGSGoods goods = goodsTable.get(goodsId);
        if (!goods.isDelisted()) {
            goods.setHeight(blockchain.getHeight());
            changePrice(goods, priceATM);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    public void changeQuantity(long goodsId, int deltaQuantity) {
        DGSGoods goods = goodsTable.get(goodsId);
        if (!goods.isDelisted()) {
            goods.setHeight(blockchain.getHeight());
            changeQuantity(goods, deltaQuantity);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    public void purchase(Transaction transaction, DigitalGoodsPurchase attachment) {
        DGSGoods goods = goodsTable.get(attachment.getGoodsId());
        if (!goods.isDelisted()
            && attachment.getQuantity() <= goods.getQuantity()
            && attachment.getPriceATM() == goods.getPriceATM()) {
            goods.setHeight(blockchain.getHeight());
            changeQuantity(goods, -attachment.getQuantity());
            DGSPurchase purchase = new DGSPurchase(transaction, attachment, goods.getSellerId(), blockchain.getLastBlockTimestamp(), new ArrayList<>());
            purchaseTable.insert(purchase);
        } else {
            Account buyer = accountService.getAccount(transaction.getSenderId());
            accountService.addToUnconfirmedBalanceATM(buyer, LedgerEvent.DIGITAL_GOODS_DELISTED, transaction.getId(),
                Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceATM()));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    public void deliver(Transaction transaction, DigitalGoodsDelivery attachment) {
        DGSPurchase purchase = getPendingPurchase(attachment.getPurchaseId());
        purchase.setHeight(blockchain.getHeight());
        setPending(purchase, false);
        long totalWithoutDiscount = Math.multiplyExact((long) purchase.getQuantity(), purchase.getPriceATM());
        Account buyer = accountService.getAccount(purchase.getBuyerId());
        long transactionId = transaction.getId();
        //buyer.addToBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId,Math.subtractExact(attachment.getDiscountATM(), totalWithoutDiscount));
        accountService.addToBalanceATM(buyer, LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, Math.subtractExact(attachment.getDiscountATM(), totalWithoutDiscount));
        //buyer.addToUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, attachment.getDiscountATM());
        accountService.addToUnconfirmedBalanceATM(buyer, LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, attachment.getDiscountATM());
        Account seller = accountService.getAccount(transaction.getSenderId());
        //seller.addToBalanceAndUnconfirmedBalanceATM(LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, Math.subtractExact(totalWithoutDiscount, attachment.getDiscountATM()));
        accountService.addToBalanceAndUnconfirmedBalanceATM(seller, LedgerEvent.DIGITAL_GOODS_DELIVERY, transactionId, Math.subtractExact(totalWithoutDiscount, attachment.getDiscountATM()));
        setEncryptedGoods(purchase, attachment.getGoods(), attachment.goodsIsText());
        setDiscountATM(purchase, attachment.getDiscountATM());

    }

    public void refund(LedgerEvent event, long eventId, long sellerId, long purchaseId, long refundATM,
                       EncryptedMessageAppendix encryptedMessage) {
        DGSPurchase purchase = purchaseTable.get(purchaseId);
        purchase.setHeight(blockchain.getHeight());
        Account seller = accountService.getAccount(sellerId);
        accountService.addToBalanceATM(seller, event, eventId, -refundATM);
        Account buyer = accountService.getAccount(purchase.getBuyerId());
        accountService.addToBalanceAndUnconfirmedBalanceATM(buyer, event, eventId, refundATM);
        if (encryptedMessage != null) {
            setRefundNote(purchase, encryptedMessage.getEncryptedData());
        }
        setRefundATM(purchase, refundATM);
    }

    public void feedback(long purchaseId, EncryptedMessageAppendix encryptedMessage, MessageAppendix message) {
        DGSPurchase purchase = purchaseTable.get(purchaseId);
        if (encryptedMessage != null) {
            addFeedback(purchase, encryptedMessage.getEncryptedData());
        }
        if (message != null) {
            addPublicFeedback(purchase, Convert.toString(message.getMessage()));
        }
    }

    public int getGoodsCount() {
        return goodsTable.getCount();
    }

    public int getGoodsCountInStock() {
        return goodsTable.getCount(inStockClause);
    }

    public DGSGoods getGoods(long goodsId) {
        return goodsTable.get(goodsId);
    }

    public DbIterator<DGSGoods> getAllGoods(int from, int to) {
        return goodsTable.getAll(from, to);
    }

    public DbIterator<DGSGoods> getGoodsInStock(int from, int to) {
        return goodsTable.getManyBy(inStockClause, from, to);
    }

    public DbIterator<DGSGoods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
        return goodsTable.getManyBy(new SellerDbClause(sellerId, inStockOnly), from, to, " ORDER BY name ASC, timestamp DESC, id ASC ");
    }

    public int getSellerGoodsCount(long sellerId, boolean inStockOnly) {
        return goodsTable.getCount(new SellerDbClause(sellerId, inStockOnly));
    }

    public void changeQuantity(DGSGoods goods, int deltaQuantity) {
        if (goods.getQuantity() == 0 && deltaQuantity > 0) {
            addTag(goods);
        }
        goods.setQuantity(goods.getQuantity() + deltaQuantity);
        if (goods.getQuantity() < 0) {
            goods.setQuantity(0);
        } else if (goods.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY) {
            goods.setQuantity(Constants.MAX_DGS_LISTING_QUANTITY);
        }
        if (goods.getQuantity() == 0) {
            delistTag(goods);
        }
        goodsTable.insert(goods);
    }

    private void changePrice(DGSGoods goods, long priceATM) {
        goods.setPriceATM(priceATM);
        goodsTable.insert(goods);
    }

    private void setDelistedGoods(DGSGoods goods, boolean delisted) {
        goods.setDelisted(delisted);
        if (goods.getQuantity() > 0) {
            delistTag(goods);
        }
        goodsTable.insert(goods);
    }

    public DbIterator<DGSGoods> searchGoods(String query, boolean inStockOnly, int from, int to) {
        return goodsTable.search(query, inStockOnly ? inStockClause : DbClause.EMPTY_CLAUSE, from, to,
            " ORDER BY ft.score DESC, goods.timestamp DESC ");
    }

    public DbIterator<DGSGoods> searchSellerGoods(String query, long sellerId, boolean inStockOnly, int from, int to) {
        return goodsTable.search(query, new SellerDbClause(sellerId, inStockOnly), from, to,
            " ORDER BY ft.score DESC, goods.name ASC, goods.timestamp DESC ");
    }

}
