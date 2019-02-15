/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
class AccountAssetTable extends VersionedEntityDbTable<AccountAsset> {
    
    private static final BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    
    private static class AccountAssetDbKeyFactory extends DbKey.LinkKeyFactory<AccountAsset> {

        public AccountAssetDbKeyFactory(String idColumnA, String idColumnB) {
            super(idColumnA, idColumnB);
        }

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }
    } 
    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new AccountAssetDbKeyFactory("account_id", "asset_id");

    public static DbKey newKey(long idA, long idB){
        return accountAssetDbKeyFactory.newKey(idA,idB);
    }
    
    public AccountAssetTable() {
        super("account_asset",accountAssetDbKeyFactory);
    }

    @Override
    protected AccountAsset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountAsset(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
         try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset " + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) " + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountAsset.accountId);
            pstmt.setLong(++i, accountAsset.assetId);
            pstmt.setLong(++i, accountAsset.quantityATU);
            pstmt.setLong(++i, accountAsset.unconfirmedQuantityATU);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }       
    }
    
    public void save(AccountAsset accountAsset) {
        Account.checkBalance(accountAsset.accountId, accountAsset.quantityATU, accountAsset.unconfirmedQuantityATU);
        if (accountAsset.quantityATU > 0 || accountAsset.unconfirmedQuantityATU > 0) {
            Account.accountAssetTable.insert(accountAsset);
        } else {
            Account.accountAssetTable.delete(accountAsset);
        }
    }
    
    @Override
    public void trim(int height) {
        super.trim(Math.max(0, height - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK));
    }

    @Override
    public void checkAvailable(int height) {
        if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < blockchainProcessor.getMinRollbackHeight()) {
            throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
        }
        if (height > Account.blockchain.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Account.blockchain.getHeight());
        }
    }

    @Override
    protected String defaultSort() {
        return " ORDER BY quantity DESC, account_id, asset_id ";
    }


    
}
