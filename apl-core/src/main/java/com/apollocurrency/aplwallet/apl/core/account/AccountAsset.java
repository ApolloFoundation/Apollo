/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class AccountAsset {
    
    private final long accountId;
    private final long assetId;
    final DbKey dbKey;
    long quantityATU;
    long unconfirmedQuantityATU;

    public AccountAsset(long accountId, long assetId, long quantityATU, long unconfirmedQuantityATU) {
        this.accountId = accountId;
        this.assetId = assetId;
        this.dbKey = Account.accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
        this.quantityATU = quantityATU;
        this.unconfirmedQuantityATU = unconfirmedQuantityATU;
    }

    public AccountAsset(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.dbKey = dbKey;
        this.quantityATU = rs.getLong("quantity");
        this.unconfirmedQuantityATU = rs.getLong("unconfirmed_quantity");
    }

    public void save(Connection con) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset " + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) " + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.quantityATU);
            pstmt.setLong(++i, this.unconfirmedQuantityATU);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

    public long getUnconfirmedQuantityATU() {
        return unconfirmedQuantityATU;
    }

    void save() {
        Account.checkBalance(this.accountId, this.quantityATU, this.unconfirmedQuantityATU);
        if (this.quantityATU > 0 || this.unconfirmedQuantityATU > 0) {
            Account.accountAssetTable.insert(this);
        } else {
            Account.accountAssetTable.delete(this);
        }
    }

    @Override
    public String toString() {
        return "AccountAsset account_id: " + Long.toUnsignedString(accountId) + " asset_id: " + Long.toUnsignedString(assetId) + " quantity: " + quantityATU + " unconfirmedQuantity: " + unconfirmedQuantityATU;
    }
    
}
