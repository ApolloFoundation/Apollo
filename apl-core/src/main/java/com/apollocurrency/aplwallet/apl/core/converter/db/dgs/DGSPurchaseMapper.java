/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.dgs;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DGSPurchaseMapper extends VersionedDerivedEntityMapper<DGSPurchase> {

    public DGSPurchaseMapper(KeyFactory<DGSPurchase> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DGSPurchase doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long buyerId = rs.getLong("buyer_id");
        long goodsId = rs.getLong("goods_id");
        long sellerId = rs.getLong("seller_id");
        int quantity = rs.getInt("quantity");
        long priceATM = rs.getLong("price");
        int deadline = rs.getInt("deadline");
        EncryptedData note = EncryptedDataUtil.loadEncryptedData(rs, "note", "nonce");
        int timestamp = rs.getInt("timestamp");
        boolean isPending = rs.getBoolean("pending");
        EncryptedData encryptedGoods = EncryptedDataUtil.loadEncryptedData(rs, "goods", "goods_nonce");
        EncryptedData refundNote = EncryptedDataUtil.loadEncryptedData(rs, "refund_note", "refund_nonce");
        boolean hasFeedbacks = rs.getBoolean("has_feedback_notes");
        boolean hasPublicFeedbacks = rs.getBoolean("has_public_feedbacks");
        long discountATM = rs.getLong("discount");
        long refundATM = rs.getLong("refund");
        boolean goodsIsText = rs.getBoolean("goods_is_text");
        return new DGSPurchase(null, null, id, buyerId, goodsId, sellerId, quantity, priceATM, deadline, note, timestamp, isPending, encryptedGoods, goodsIsText, refundNote, hasPublicFeedbacks, hasFeedbacks, null, null, discountATM, refundATM);

    }
}
