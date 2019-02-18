/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
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
public class AccountAssetTable extends VersionedEntityDbTable<AccountAsset> {
    
    private static class AccountAssetDbKeyFactory extends LinkKeyFactory<AccountAsset> {

        public AccountAssetDbKeyFactory(String idColumnA, String idColumnB) {
            super(idColumnA, idColumnB);
        }

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }
    } 
    private static final LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new AccountAssetDbKeyFactory("account_id", "asset_id");
    private  static final AccountAssetTable accountAssetTable = new AccountAssetTable();   
    private static final BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    
    public static DbKey newKey(long idA, long idB){
        return accountAssetDbKeyFactory.newKey(idA,idB);
    }
    
    public static AccountAssetTable getInstance(){
        return accountAssetTable;
    }
    private AccountAssetTable() {
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
            accountAssetTable.insert(accountAsset);
        } else {
            accountAssetTable.delete(accountAsset);
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

    public static int getAssetAccountCount(long assetId) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public static int getAssetAccountCount(long assetId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId), height);
    }

    public static int getAccountAssetCount(long accountId) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountAssetCount(long accountId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId) {
        return accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId));
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId, int height) {
        return accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId), height);
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

  
    public static long getAssetBalanceATU(long accountId, long assetId, int height) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityATU;
    }

    public static long getUnconfirmedAssetBalanceATU(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }
  
}
