/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.asset;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTransferTable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString(callSuper = true)
@Getter
public class AssetTransfer extends DerivedEntity {

    private long id;
    @Setter
    private long assetId;
    @Setter
    private long senderId;
    @Setter
    private long recipientId;
    @Setter
    private long quantityATM;
    private int timestamp;

    public AssetTransfer(Transaction transaction, ColoredCoinsAssetTransfer attachment, int timestamp, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.setDbKey(AssetTransferTable.assetTransferDbKeyFactory.newKey(this.id));
        this.assetId = attachment.getAssetId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.quantityATM = attachment.getQuantityATU();
        this.timestamp = timestamp;
    }

    /**
     * for unit tests
     */
    public AssetTransfer(long id, long assetId, long senderAccountId, long recipientAccountId,
                         int quantity, int timestamp, int height) {
        super(null, height);
        this.id = id;
        this.assetId = assetId;
        this.senderId = senderAccountId;
        this.recipientId = recipientAccountId;
        this.quantityATM = quantity;
        this.timestamp = timestamp;
    }

    public AssetTransfer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        setDbKey(dbKey);
        this.assetId = rs.getLong("asset_id");
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.quantityATM = rs.getLong("quantity");
        this.timestamp = rs.getInt("timestamp");
        this.setHeight(rs.getInt("height"));
    }

}
