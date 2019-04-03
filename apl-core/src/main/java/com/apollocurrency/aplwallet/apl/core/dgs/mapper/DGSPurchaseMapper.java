/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.dgs.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;


public class DGSPurchaseMapper implements RowMapper<DGSPurchase> {
    @Override
    public DGSPurchase map(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long buyerId = rs.getLong("buyer_id");
        long goodsId = rs.getLong("goods_id");
        long sellerId = rs.getLong("seller_id");
        int quantity = rs.getInt("quantity");
        Long priceATM = rs.getLong("price");
        int deadline = rs.getInt("deadline");
        EncryptedData note = EncryptedDataUtil.loadEncryptedData(rs, "note", "nonce");
        int timestamp = rs.getInt("timestamp");
        boolean isPending = rs.getBoolean("pending");
        EncryptedData encryptedGoods = EncryptedDataUtil.loadEncryptedData(rs, "goods", "goods_nonce");
        EncryptedData refundNote = EncryptedDataUtil.loadEncryptedData(rs, "refund_note", "refund_nonce");
        boolean hasFeedbackNotes = rs.getBoolean("has_feedback_notes");
        boolean hasPublicFeedbacks = rs.getBoolean("has_public_feedbacks");
        Long discountATM = rs.getLong("discount");
        Long refundATM = rs.getLong("refund");
        boolean goodsIsText = rs.getBoolean("goods_is_text");
        return new DGSPurchase(id, buyerId, goodsId, sellerId, quantity, priceATM, deadline, note, timestamp, isPending, encryptedGoods, goodsIsText, refundNote, hasFeedbackNotes, Collections.emptyList(), hasPublicFeedbacks, Collections.emptyList(), discountATM, refundATM);
    }
}
