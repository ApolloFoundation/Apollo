/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.MinMaxIdMapper;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class ShardDaoJdbcImpl implements ShardDaoJdbc {
    private static final Logger log = getLogger(ShardDaoJdbcImpl.class);

    private MinMaxIdMapper rowMapper = new MinMaxIdMapper();

    public ShardDaoJdbcImpl() {
    }

    public MinMaxDbId getMinMaxId(TransactionalDataSource sourceDataSource, long height) throws SQLException {
        Objects.requireNonNull(sourceDataSource,"sourceDataSource is NULL");
        try ( Connection con = sourceDataSource.getConnection();
              PreparedStatement pstmt = con.prepareStatement(
                      "SELECT IFNULL(min(SHARD_ID), 0) as MIN_ID, IFNULL(max(SHARD_ID), 0) as MAX_ID, IFNULL(count(*), 0) as COUNT, max(shard_height) as max_height" +
                " FROM shard WHERE shard_height <=?")) {
            pstmt.setLong(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                return getIfPresent(rs);
            }
        } catch (SQLException e) {
            log.error("getLatest recovery error", e);
            throw e;
        }
    }

    private MinMaxDbId getIfPresent(ResultSet rs) throws SQLException {
        MinMaxDbId minMaxDbId = null;
        if (rs.next()) {
            minMaxDbId = rowMapper.map(rs, null);
        }
        log.trace("Retrieved MinMaxDbID = {}", minMaxDbId);
        return minMaxDbId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet getRangeByDbId(Connection con, PreparedStatement pstmt,
                                    MinMaxDbId minMaxDbId, int limit) throws SQLException {
        Objects.requireNonNull(con, "connnection is NULL");
        Objects.requireNonNull(pstmt, "prepared statement is NULL");
        Objects.requireNonNull(minMaxDbId, "minMaxDbId is NULL");
        try {
            pstmt.setLong(1, minMaxDbId.getMinDbId());
            pstmt.setLong(2, minMaxDbId.getMaxDbId());
            pstmt.setLong(3, limit);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            throw e;
        }
    }

}
