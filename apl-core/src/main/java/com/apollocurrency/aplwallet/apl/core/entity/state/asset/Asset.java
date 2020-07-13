/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.asset;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class Asset extends VersionedDerivedEntity {

    private long id;
    private long accountId;
    private String name;
    private String description;
    private long initialQuantityATU;
    private byte decimals;
    private long quantityATU;

    public Asset(Transaction transaction, ColoredCoinsAssetIssuance attachment, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.setDbKey(AssetTable.assetDbKeyFactory.newKey(this.id));
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
    public Asset(long id, long senderAccountId, ColoredCoinsAssetIssuance attachment, int height) {
        super(null, height);
        this.id = id;
        this.accountId = senderAccountId;
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityATU = attachment.getQuantityATU();
        this.initialQuantityATU = this.quantityATU;
        this.decimals = attachment.getDecimals();
    }

    public Asset(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        setDbKey(dbKey);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.initialQuantityATU = rs.getLong("initial_quantity");
        this.quantityATU = rs.getLong("quantity");
        this.decimals = rs.getByte("decimals");
    }

}
