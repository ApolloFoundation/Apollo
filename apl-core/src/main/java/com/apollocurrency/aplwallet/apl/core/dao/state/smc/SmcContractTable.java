/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SmcContractTable extends VersionedDeletableEntityDbTable<SmcContractEntity> {
    public static final LongKeyFactory<SmcContractEntity> KEY_FACTORY = new LongKeyFactory<>("address") {
        @Override
        public DbKey newKey(SmcContractEntity contract) {
            if (contract.getDbKey() == null) {
                contract.setDbKey(newKey(contract.getAddress()));
            }
            return contract.getDbKey();
        }
    };

    private static final String TABLE_NAME = "smc_contract";

    private static final SmcContractRowMapper MAPPER = new SmcContractRowMapper(KEY_FACTORY);

    @Inject
    public SmcContractTable(DatabaseManager databaseManager, Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, null, databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    protected SmcContractEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractEntity value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContractEntity entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
                "(address, owner, transaction_id, data, name, language, version, args, status, height, latest) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)"
            , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, entity.getAddress());
            pstmt.setLong(++i, entity.getOwner());
            pstmt.setLong(++i, entity.getTransactionId());
            pstmt.setString(++i, entity.getData());
            pstmt.setString(++i, entity.getContractName());
            pstmt.setString(++i, entity.getLanguageName());
            pstmt.setString(++i, entity.getLanguageVersion());
            pstmt.setString(++i, entity.getArgs());
            pstmt.setString(++i, entity.getStatus());
            pstmt.setInt(i, entity.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setDbId(rs.getLong(1));
                }
            }
        }
    }
}
