/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString
@Getter
@Setter
@EqualsAndHashCode
public class Trade {
    private final int timestamp;
    private final long assetId;
    private final long blockId;
    private final int height;
    private final long askOrderId;
    private final long bidOrderId;
    private final int askOrderHeight;
    private final int bidOrderHeight;
    private final long sellerId;
    private final long buyerId;
    private final long quantityATU;
    private final long priceATM;
    private final boolean isBuy;
    private DbKey dbKey;

    public Trade(
        final long assetId,
        final AskOrder askOrder,
        final BidOrder bidOrder,
        final DbKey dbKey,
        final long blockId,
        final int height,
        final int timestamp
    ) {
        this.blockId = blockId;
        this.height = height;
        this.assetId = assetId;
        this.timestamp = timestamp;
        this.askOrderId = askOrder.getId();
        this.bidOrderId = bidOrder.getId();
        this.askOrderHeight = askOrder.getCreationHeight();
        this.bidOrderHeight = bidOrder.getCreationHeight();
        this.sellerId = askOrder.getAccountId();
        this.buyerId = bidOrder.getAccountId();
        this.dbKey = dbKey;
        this.quantityATU = Math.min(askOrder.getQuantityATU(), bidOrder.getQuantityATU());
        this.isBuy = askOrderHeight < bidOrderHeight ||
            askOrderHeight == bidOrderHeight &&
                (askOrder.getTransactionHeight() < bidOrder.getTransactionHeight() ||
                    (askOrder.getTransactionHeight() == bidOrder.getTransactionHeight() && askOrder.getTransactionIndex() < bidOrder.getTransactionIndex()));
        this.priceATM = isBuy ? askOrder.getPriceATM() : bidOrder.getPriceATM();
    }

    public Trade(final ResultSet rs, final DbKey dbKey) throws SQLException {
        this.assetId = rs.getLong("asset_id");
        this.blockId = rs.getLong("block_id");
        this.askOrderId = rs.getLong("ask_order_id");
        this.bidOrderId = rs.getLong("bid_order_id");
        this.askOrderHeight = rs.getInt("ask_order_height");
        this.bidOrderHeight = rs.getInt("bid_order_height");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = dbKey;
        this.quantityATU = rs.getLong("quantity");
        this.priceATM = rs.getLong("price");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
        this.isBuy = rs.getBoolean("is_buy");
    }

}