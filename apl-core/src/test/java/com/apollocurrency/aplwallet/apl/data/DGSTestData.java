/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.List;

public class DGSTestData {

    private DGSPublicFeedback createFeedback(long dbId, long id, String publicFeedback, int height, boolean latest) {
        DGSPublicFeedback dgsPublicFeedback = new DGSPublicFeedback(dbId, height, publicFeedback, id);
        dgsPublicFeedback.setLatest(latest);
        return dgsPublicFeedback;
    }

    public final DGSPublicFeedback PUBLIC_FEEDBACK_0 = createFeedback(1, 100,                   "Deleted feedback",       541960, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_1 = createFeedback(2, 7052449049531083429L, "Feedback message",        541965, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_2 = createFeedback(3, -3910276708092716563L, "Goods dgs",              542677, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_3 = createFeedback(4, -3910276708092716563L, "Goods dgs",              542679, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_4 = createFeedback(5, -3910276708092716563L, "Test goods feedback 2",  542679, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_5 = createFeedback(6, -1155069143692520623L, "Public feedback",        542693, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_6 = createFeedback(7, -1155069143692520623L, "Public feedback",        542695, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_7 = createFeedback(8, -1155069143692520623L, "Another Public feedback",542695, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_8 = createFeedback(9, 100,                   "Deleted feedback",       542695, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_10 = createFeedback(11, 7052449049531083429L, "Public feedback 2",     542799, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_9 = createFeedback(10, 7052449049531083429L, "Feedback message",       542799, false);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_11 = createFeedback(12, 7052449049531083429L, "Feedback message",      542801, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_12 = createFeedback(13, 7052449049531083429L, "Public feedback 2",     542801, true);
    public final DGSPublicFeedback PUBLIC_FEEDBACK_13 = createFeedback(14, 7052449049531083429L, "Public feedback 3",     542801, true);

    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_0 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 1, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-1", PUBLIC_FEEDBACK_13.getId());
    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_1 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 2, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-2", PUBLIC_FEEDBACK_13.getId());
    public final DGSPublicFeedback NEW_PUBLIC_FEEDBACK_2 = new DGSPublicFeedback(PUBLIC_FEEDBACK_13.getDbId() + 3, PUBLIC_FEEDBACK_13.getHeight() + 1, "NewFeedback-3", PUBLIC_FEEDBACK_13.getId());

//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()
//    public final DGSPurchase PURCHASE_ = new DGSPurchase()


    public DGSPurchase createPurchase(long dbId, long id, long buyerId, long goodsId, long sellerId, int quantity, long price, short deadline, String note, String nonce, int timestamp, boolean pending, String goods, String goodsNonce, boolean goodsIsText, String refundNote, String refundNonce, boolean hasFeedbackNotes, boolean hasPublicFeedbacks, long discount, long refund, int height, boolean latest, List<DGSPublicFeedback> publicFeedbacks, List<DGSFeedback> feedbacks) {
        DGSPurchase dgsPurchase = new DGSPurchase(dbId, height, id, buyerId, goodsId, sellerId, quantity, price, deadline, new EncryptedData(Convert.parseHexString(note), Convert.parseHexString(nonce)), timestamp, pending, new EncryptedData(Convert.parseHexString(goods), Convert.parseHexString(goodsNonce)), goodsIsText, new EncryptedData(Convert.parseHexString(refundNonce), Convert.parseHexString(refundNonce)), hasPublicFeedbacks, hasFeedbackNotes, feedbacks, publicFeedbacks, discount, refund);
        dgsPurchase.setLatest(latest);
        return dgsPurchase;
    }
    
}
