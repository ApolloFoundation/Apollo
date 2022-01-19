/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractMappingRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.ThreeValuesKeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
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
 * Smart-contract mapping table
 *
 * @author andrew.zinchenko@gmail.com
 */
public class SmcContractMappingTable extends VersionedDeletableEntityDbTable<SmcContractMappingEntity> {
    public static final ThreeValuesKeyFactory<SmcContractMappingEntity> KEY_FACTORY = new ThreeValuesKeyFactory<>("address", "name", "entry_key") {
        @Override
        public DbKey newKey(SmcContractMappingEntity mapping) {
            if (mapping.getDbKey() == null) {
                mapping.setDbKey(super.newKey(mapping.getAddress(), mapping.getName(), mapping.getKey()));
            }
            return mapping.getDbKey();
        }
    };
    private static final String TABLE_NAME = "smc_mapping";

    private static final SmcContractMappingRowMapper MAPPER = new SmcContractMappingRowMapper(KEY_FACTORY);

    @Inject
    public SmcContractMappingTable(DatabaseManager databaseManager, Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, null, databaseManager, fullTextOperationDataEvent);
    }

    @Override
    protected SmcContractMappingEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractMappingEntity value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContractMappingEntity entity) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
                    "(address, entry_key, name, object, height, latest) " +
                    "VALUES (?, ?, ?, ?, ?, TRUE) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "address = VALUES(address), entry_key = VALUES(entry_key), name = VALUES(name), object = VALUES(object), height = VALUES(height), latest = TRUE"
                , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, entity.getAddress());
            pstmt.setBytes(++i, entity.getKey());
            pstmt.setString(++i, entity.getName());
            pstmt.setString(++i, entity.getSerializedObject());
            pstmt.setInt(++i, entity.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setDbId(rs.getLong(1));
                }
            }
        }
    }
}
