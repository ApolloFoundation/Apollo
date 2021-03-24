/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractStateRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

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
public class SmcContractStateTable extends VersionedDeletableEntityDbTable<SmcContractStateEntity> {
    public static final LongKeyFactory<SmcContractStateEntity> KEY_FACTORY = new LongKeyFactory<>("address") {
        @Override
        public DbKey newKey(SmcContractStateEntity contract) {
            if (contract.getDbKey() == null) {
                contract.setDbKey(newKey(contract.getAddress()));
            }
            return contract.getDbKey();
        }
    };

    private static final String TABLE_NAME = "smc_state";

    private static final SmcContractStateRowMapper MAPPER = new SmcContractStateRowMapper(KEY_FACTORY);

    @Inject
    public SmcContractStateTable(DatabaseManager databaseManager, Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, null, databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    protected SmcContractStateEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractStateEntity value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContractStateEntity entity) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
                    "(address, object, status, height, latest) " +
                    "VALUES (?, ?, ?, ?, ?, ?,  ?, TRUE) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "address = VALUES(address), object = VALUES(object), status = VALUES(status), height = VALUES(height), latest = TRUE"
                , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, entity.getAddress());
            pstmt.setString(++i, entity.getSerializedObject());
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
