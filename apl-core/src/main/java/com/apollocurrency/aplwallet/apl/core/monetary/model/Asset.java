/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetTable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class Asset extends VersionedDerivedEntity {

    private long assetId;
    private long accountId;
    private String name;
    private String description;
    private long initialQuantityATU;
    private byte decimals;
    private long quantityATU;

    public Asset(Transaction transaction, ColoredCoinsAssetIssuance attachment, int height) {
        super(null, height);
        this.assetId = transaction.getId();
        this.setDbKey(AssetTable.assetDbKeyFactory.newKey(this.assetId));
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityATU = attachment.getQuantityATU();
        this.initialQuantityATU = this.quantityATU;
        this.decimals = attachment.getDecimals();
    }

    /**
     * For unit tests
     */
    public Asset(long assetId, long senderAccountId, ColoredCoinsAssetIssuance attachment, int height) {
        super(null, height);
        this.assetId = assetId;
//        this.setDbKey(AssetTable.assetDbKeyFactory.newKey(this.assetId));
        this.accountId = senderAccountId;
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityATU = attachment.getQuantityATU();
        this.initialQuantityATU = this.quantityATU;
        this.decimals = attachment.getDecimals();
    }

    public Asset(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.assetId = rs.getLong("id");
        setDbKey(dbKey);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.initialQuantityATU = rs.getLong("initial_quantity");
        this.quantityATU = rs.getLong("quantity");
        this.decimals = rs.getByte("decimals");
    }

}
