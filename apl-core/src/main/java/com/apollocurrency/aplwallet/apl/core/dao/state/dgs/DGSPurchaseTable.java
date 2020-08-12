/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSPurchaseMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.utils.EncryptedDataUtil;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class DGSPurchaseTable extends EntityDbTable<DGSPurchase> {
    private static final LongKeyFactory<DGSPurchase> KEY_FACTORY = new LongKeyFactory<DGSPurchase>("id") {
        @Override
        public DbKey newKey(DGSPurchase purchase) {
            if (purchase.getDbKey() == null) {
                long id = purchase.getId();
                purchase.setDbKey(new LongKey(id));
            }
            return purchase.getDbKey();
        }
    };
    private static final DGSPurchaseMapper MAPPER = new DGSPurchaseMapper(KEY_FACTORY);
    private static final String TABLE = "purchase";

    public DGSPurchaseTable() {
        super(TABLE, KEY_FACTORY, true, null, false);
    }

    @Override
    public DGSPurchase load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSPurchase purchase = MAPPER.map(rs, null);
        purchase.setDbKey(dbKey);
        return purchase;
    }

    @Override
    public void save(Connection con, DGSPurchase purchase) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO purchase (id, buyer_id, goods_id, seller_id, "
                + "quantity, price, deadline, note, nonce, `timestamp`, pending, goods, goods_nonce, goods_is_text, refund_note, "
                + "refund_nonce, has_feedback_notes, has_public_feedbacks, discount, refund, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), buyer_id = VALUES(buyer_id), goods_id = VALUES(goods_id), "
                + "seller_id = VALUES(seller_id), quantity = VALUES(quantity), price = VALUES(price), deadline = VALUES(deadline), "
                + "note = VALUES(note), nonce = VALUES(nonce), `timestamp` = VALUES(`timestamp`), pending = VALUES(pending), "
                + "goods = VALUES(goods), goods_nonce = VALUES(goods_nonce), goods_is_text = VALUES(goods_is_text), "
                + "refund_note = VALUES(refund_note), refund_nonce = VALUES(refund_nonce), has_feedback_notes = VALUES(has_feedback_notes), "
                + "has_public_feedbacks = VALUES(has_public_feedbacks), discount = VALUES(discount), refund = VALUES(refund), "
                + "height = VALUES(height), latest = TRUE")
        ) {
            int i = 0;
            pstmt.setLong(++i, purchase.getId());
            pstmt.setLong(++i, purchase.getBuyerId());
            pstmt.setLong(++i, purchase.getGoodsId());
            pstmt.setLong(++i, purchase.getSellerId());
            pstmt.setInt(++i, purchase.getQuantity());
            pstmt.setLong(++i, purchase.getPriceATM());
            pstmt.setInt(++i, purchase.getDeadline());
            i = EncryptedDataUtil.setEncryptedData(pstmt, purchase.getNote(), ++i);
            pstmt.setInt(i, purchase.getTimestamp());
            pstmt.setBoolean(++i, purchase.isPending());
            i = EncryptedDataUtil.setEncryptedData(pstmt, purchase.getEncryptedGoods(), ++i);
            pstmt.setBoolean(i, purchase.isGoodsIsText());
            i = EncryptedDataUtil.setEncryptedData(pstmt, purchase.getRefundNote(), ++i);
            pstmt.setBoolean(i, purchase.hasFeedbacks());
            pstmt.setBoolean(++i, purchase.hasPublicFeedbacks());
            pstmt.setLong(++i, purchase.getDiscountATM());
            pstmt.setLong(++i, purchase.getRefundATM());
            pstmt.setInt(++i, purchase.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DGSPurchase get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
    public String defaultSort() {
        return " ORDER BY timestamp DESC, id ASC ";
    }

}
