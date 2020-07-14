/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;

import java.util.List;


public interface DGSService {

    int getPurchaseCount();

    int getPurchaseCount(boolean withPublicFeedbacksOnly, boolean completedOnly);

    DbIterator<DGSPurchase> getAllPurchases(int from, int to);

    DbIterator<DGSPurchase> getPurchases(boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to);

    DbIterator<DGSPurchase> getSellerPurchases(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to);

    int getSellerPurchaseCount(long sellerId, boolean withPublicFeedbacksOnly, boolean completedOnly);


    DbIterator<DGSPurchase> getBuyerPurchases(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to);

    int getBuyerPurchaseCount(long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly);

    DbIterator<DGSPurchase> getSellerBuyerPurchases(final long sellerId, final long buyerId,
                                                    boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to);

    int getSellerBuyerPurchaseCount(final long sellerId, final long buyerId,
                                    boolean withPublicFeedbacksOnly, boolean completedOnly);

    DbIterator<DGSPurchase> getGoodsPurchases(long goodsId, long buyerId, boolean withPublicFeedbacksOnly, boolean completedOnly, int from, int to);

    int getGoodsPurchaseCount(final long goodsId, boolean withPublicFeedbacksOnly, boolean completedOnly);

    DGSPurchase getPurchase(long purchaseId);

    DbIterator<DGSPurchase> getPendingSellerPurchases(final long sellerId, int from, int to);

    DbIterator<DGSPurchase> getExpiredSellerPurchases(final long sellerId, int from, int to);

    DGSPurchase getPendingPurchase(long purchaseId);

    DbIterator<DGSPurchase> getExpiredPendingPurchases(Block block);

    void setPending(DGSPurchase purchase, boolean isPending);


    List<DGSFeedback> getFeedbacks(DGSPurchase dgsPurchase);

    List<DGSPublicFeedback> getPublicFeedbacks(DGSPurchase dgsPurchase);

    void listGoods(Transaction transaction, DigitalGoodsListing attachment);

    int getTagsCount();

    int getCountInStock();

    DbIterator<DGSTag> getAllTags(int from, int to);

    DbIterator<DGSTag> getInStockTags(int from, int to);

    DbIterator<DGSTag> getTagsLike(String prefix, boolean inStockOnly, int from, int to);


    void delistGoods(long goodsId);

    void changePrice(long goodsId, long priceATM);

    void changeQuantity(long goodsId, int deltaQuantity);

    void purchase(Transaction transaction, DigitalGoodsPurchase attachment);

    void deliver(Transaction transaction, DigitalGoodsDelivery attachment);

    void refund(LedgerEvent event, long eventId, long sellerId, long purchaseId, long refundATM,
                EncryptedMessageAppendix encryptedMessage);

    void feedback(long purchaseId, EncryptedMessageAppendix encryptedMessage, MessageAppendix message);


    int getGoodsCount();

    int getGoodsCountInStock();

    DGSGoods getGoods(long goodsId);

    DbIterator<DGSGoods> getAllGoods(int from, int to);

    DbIterator<DGSGoods> getGoodsInStock(int from, int to);

    DbIterator<DGSGoods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to);

    int getSellerGoodsCount(long sellerId, boolean inStockOnly);

    void changeQuantity(DGSGoods goods, int deltaQuantity);


    DbIterator<DGSGoods> searchGoods(String query, boolean inStockOnly, int from, int to);

    DbIterator<DGSGoods> searchSellerGoods(String query, long sellerId, boolean inStockOnly, int from, int to);
}

