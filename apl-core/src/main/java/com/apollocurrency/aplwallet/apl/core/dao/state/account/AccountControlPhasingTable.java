/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class AccountControlPhasingTable extends VersionedDeletableEntityDbTable<AccountControlPhasing> {

    public static final LongKeyFactory<AccountControlPhasing> accountControlPhasingDbKeyFactory =
        new LongKeyFactory<>("account_id") {
            @Override
            public DbKey newKey(AccountControlPhasing accountControlPhasing) {
                if (accountControlPhasing.getDbKey() == null) {
                    accountControlPhasing.setDbKey(super.newKey(accountControlPhasing.getAccountId()));
                }
                return accountControlPhasing.getDbKey();
            }
        };

    @Inject
    public AccountControlPhasingTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                      DatabaseManager databaseManager,
                                      Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("account_control_phasing", accountControlPhasingDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public AccountControlPhasing load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountControlPhasing(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountControlPhasing phasingOnly) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement(
                "MERGE INTO account_control_phasing " +
                    "(account_id, whitelist, voting_model, quorum, min_balance, holding_id, min_balance_model, "
                    + "max_fees, min_duration, max_duration, height, latest, deleted) KEY (account_id, height) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, phasingOnly.getAccountId());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, Convert.toArray(phasingOnly.getPhasingParams().getWhitelist()));
            pstmt.setByte(++i, phasingOnly.getPhasingParams().getVoteWeighting().getVotingModel().getCode());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingOnly.getPhasingParams().getQuorum());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingOnly.getPhasingParams().getVoteWeighting().getMinBalance());
            DbUtils.setLongZeroToNull(pstmt, ++i, phasingOnly.getPhasingParams().getVoteWeighting().getHoldingId());
            pstmt.setByte(++i, phasingOnly.getPhasingParams().getVoteWeighting().getMinBalanceModel().getCode());
            pstmt.setLong(++i, phasingOnly.getMaxFees());
            pstmt.setShort(++i, phasingOnly.getMinDuration());
            pstmt.setShort(++i, phasingOnly.getMaxDuration());
            pstmt.setInt(++i, phasingOnly.getHeight());
            pstmt.executeUpdate();
        }
    }

}