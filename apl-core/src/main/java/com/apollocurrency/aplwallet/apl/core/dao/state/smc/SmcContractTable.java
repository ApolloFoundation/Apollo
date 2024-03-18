/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractDetailsRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.JdbcQueryExecutionHelper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractQuery;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Smart-contract master table
 *
 * @author andrew.zinchenko@gmail.com
 */
public class SmcContractTable extends EntityDbTable<SmcContractEntity> {
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
    private final JdbcQueryExecutionHelper<ContractDetails> txQueryExecutionHelper;

    @Inject
    public SmcContractTable(DatabaseManager databaseManager, Event<FullTextOperationData> fullTextOperationDataEvent, SmcContractDetailsRowMapper smcContractDetailsRowMapper) {
        super(TABLE_NAME, KEY_FACTORY, false, null, databaseManager, fullTextOperationDataEvent);
        this.txQueryExecutionHelper = new JdbcQueryExecutionHelper<>(databaseManager.getDataSource(), (rs) -> smcContractDetailsRowMapper.map(rs, null));
    }

    @Override
    protected SmcContractEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        SmcContractEntity value = MAPPER.map(rs, null);
        value.setDbKey(dbKey);
        return value;
    }

    /**
     * Insert new entry only, the update is not supported
     *
     * @param entity smart contract entry
     */
    @Override
    public void insert(SmcContractEntity entity) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {

            save(con, entity);

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void save(Connection con, SmcContractEntity entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
                "(address, owner, transaction_id, transaction_full_hash, fuel_price, fuel_limit, fuel_charged, block_timestamp, data, name, base_contract, args, language, version, status, height) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            , Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setLong(++i, entity.getAddress());
            pstmt.setLong(++i, entity.getOwner());
            pstmt.setLong(++i, entity.getTransactionId());
            pstmt.setBytes(++i, entity.getTransactionHash());
            pstmt.setLong(++i, entity.getFuelPrice());
            pstmt.setLong(++i, entity.getFuelLimit());
            pstmt.setLong(++i, entity.getFuelCharged());
            pstmt.setInt(++i, entity.getBlockTimestamp());
            pstmt.setString(++i, entity.getData());
            pstmt.setString(++i, entity.getContractName());
            pstmt.setString(++i, entity.getBaseContract());
            pstmt.setString(++i, entity.getArgs());
            pstmt.setString(++i, entity.getLanguageName());
            pstmt.setString(++i, entity.getLanguageVersion());
            pstmt.setString(++i, entity.getStatus());
            pstmt.setInt(++i, entity.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setDbId(rs.getLong(1));
                }
            }
        }
    }

    public List<ContractDetails> getContractsByFilter(ContractQuery query) {
        StringBuilder sql = new StringBuilder(
            "SELECT sc.*, " +
                "ss.status as smc_status " +
                "FROM smc_contract sc " +
                "LEFT JOIN smc_state ss on sc.address = ss.address ");
        sql.append(query.toWhereClause("WHERE sc.latest = true AND ss.latest = true "));
        sql.append("ORDER BY sc.block_timestamp DESC, sc.db_id DESC ");
        sql.append(DbUtils.limitsClause(query.getPaging()));

        return txQueryExecutionHelper.executeListQuery(con -> {
            PreparedStatement pstm = con.prepareStatement(sql.toString());
            query.setPreparedStatementParameters(pstm);
            pstm.setFetchSize(50);
            return pstm;
        });
    }
}
