/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractEventRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventEntity;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Account ledger table
 */
@Singleton
public class SmcContractEventTable extends EntityDbTable<SmcContractEventEntity> {
    public static final LongKeyFactory<SmcContractEventEntity> KEY_FACTORY = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(SmcContractEventEntity entity) {
            if (entity.getDbKey() == null) {
                entity.setDbKey(super.newKey(entity.getId()));
            }
            return entity.getDbKey();
        }
    };
    private static final String TABLE_NAME = "smc_event";

    private static final SmcContractEventRowMapper MAPPER = new SmcContractEventRowMapper(KEY_FACTORY);

    private final PropertiesHolder propertiesHolder;
    private final int batchCommitSize;

    /**
     * Create the Smc Contract Event Type table
     */
    @Inject
    public SmcContractEventTable(PropertiesHolder propertiesHolder, DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, false, null, databaseManager, deleteOnTrimDataEvent);
        this.propertiesHolder = propertiesHolder;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }

    @Override
    protected SmcContractEventEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractEventEntity value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    @Override
    public void save(Connection con, SmcContractEventEntity eventEntity) throws SQLException {
        try (final PreparedStatement stmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
            " (id, transaction_id, contract, signature, name, idx_count, is_anonymous, height) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            stmt.setLong(++i, eventEntity.getId());
            stmt.setLong(++i, eventEntity.getTransactionId());
            stmt.setLong(++i, eventEntity.getContract());
            stmt.setBytes(++i, eventEntity.getSignature());
            stmt.setString(++i, eventEntity.getName());
            stmt.setByte(++i, eventEntity.getIdxCount());
            stmt.setBoolean(++i, eventEntity.isAnonymous());
            stmt.setInt(++i, eventEntity.getHeight());
            stmt.executeUpdate();
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    eventEntity.setDbId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void trim(int height) {
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT)
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + TABLE_NAME
                 + " WHERE height < ? AND height >= 0 LIMIT " + batchCommitSize)) {
            pstmtDelete.setInt(1, height);
            int count;
            do {
                count = pstmtDelete.executeUpdate();
                dataSource.commit(false);
            } while (count >= batchCommitSize);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
