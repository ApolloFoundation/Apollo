/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.operation.order;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
@ToString(callSuper = true)
public abstract class Order extends VersionedDerivedEntity {
    private final long id;
    private final long accountId;
    private final long assetId;
    private final long priceATM;
    private final int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;
    private long quantityATU;

    public Order(Transaction transaction, ColoredCoinsOrderPlacementAttachment attachment, int height) {
        super(transaction.getId(), height);
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.assetId = attachment.getAssetId();
        this.quantityATU = attachment.getQuantityATU();
        this.priceATM = attachment.getPriceATM();
        this.creationHeight = height;
        this.transactionIndex = transaction.getIndex();
        this.transactionHeight = transaction.getHeight();
    }

    public Order(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.priceATM = rs.getLong("price");
        this.quantityATU = rs.getLong("quantity");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
        setDbKey(dbKey);
    }
}
