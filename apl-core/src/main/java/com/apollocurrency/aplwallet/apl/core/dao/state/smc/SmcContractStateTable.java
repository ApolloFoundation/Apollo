/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractStateMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkStrKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractState;
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
public class SmcContractStateTable extends VersionedDeletableEntityDbTable<SmcContractState> {
    public static final LinkStrKeyFactory<SmcContractState> KEY_FACTORY = new LinkStrKeyFactory<>("address", "transaction_id") {
        @Override
        public DbKey newKey(SmcContractState contract) {
            if (contract.getDbKey() == null) {
                contract.setDbKey(newKey(contract.getAddress(), contract.getTransactionId()));
            }
            return contract.getDbKey();
        }
    };

    private static final String TABLE_NAME = "smc_state";

    private static final SmcContractStateMapper MAPPER = new SmcContractStateMapper(KEY_FACTORY);

    @Inject
    public SmcContractStateTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                 DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent){
        super(TABLE_NAME, KEY_FACTORY,
            null, derivedDbTablesRegistry, databaseManager,
            null, deleteOnTrimDataEvent );

    }

    @Override
    protected SmcContractState load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractState value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContractState entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
                "(address, transaction_id, method, args, object, status, height, latest) "+
                "VALUES (?, ?, ?, ?, ?, ?, ?,  ?, TRUE)"
            , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setString(++i, entity.getAddress());
            pstmt.setString(++i, entity.getTransactionId());
            pstmt.setString(++i, entity.getMethod());
            pstmt.setString(++i, entity.getArgs());
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
