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
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
class AccountAssetTable extends VersionedEntityDbTable<AccountAsset> {
    private static final BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    
    public AccountAssetTable(String table, DbKey.Factory<AccountAsset> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    @Override
    protected AccountAsset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountAsset(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
        accountAsset.save(con);
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
