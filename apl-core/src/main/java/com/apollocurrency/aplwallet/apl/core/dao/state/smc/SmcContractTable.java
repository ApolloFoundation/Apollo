/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContract;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
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
public class SmcContractTable extends VersionedDeletableEntityDbTable<SmcContract> {
    public static final StringKeyFactory<SmcContract> KEY_FACTORY = new StringKeyFactory<>("address") {
        @Override
        public DbKey newKey(SmcContract contract) {
            if (contract.getDbKey() == null) {
                contract.setDbKey(newKey(contract.getAddress()));
            }
            return contract.getDbKey();
        }
    };

    private static final String TABLE_NAME = "smc_contract";

    private static final SmcContractMapper MAPPER = new SmcContractMapper(KEY_FACTORY);

    @Inject
    public SmcContractTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                 DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent){
        super(TABLE_NAME, KEY_FACTORY,
            null, derivedDbTablesRegistry, databaseManager,
            null, deleteOnTrimDataEvent );
    }

    @Override
    protected SmcContract load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContract value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContract entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
            "(address, data, name, language, fuel, fuel_price, transaction_id, height, latest) "+
            "VALUES (?, ?, ?, ?, ?, ?, ?,  ?, TRUE)"
            , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setString(++i, entity.getAddress());
            pstmt.setString(++i, entity.getData());
            pstmt.setString(++i, entity.getContractName());
            pstmt.setString(++i, entity.getLanguageName());
            pstmt.setString(++i, entity.getFuelValue().toString());
            pstmt.setString(++i, entity.getFuelPrice().toString());
            pstmt.setString(++i, entity.getTransactionId());
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
