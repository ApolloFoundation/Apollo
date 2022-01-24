/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.api.PositiveRange;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import lombok.Builder;
import lombok.Data;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class ContractQuery {
    Long address;
    Long transaction;
    Long owner;
    String name;
    String baseContract;
    Long timestamp;
    String status;
    int height;
    Sort order;
    PositiveRange paging;

    public String toWhereClause(String wherePrefix) {
        return toWhereClause(new StringBuilder(wherePrefix)).toString();
    }

    public StringBuilder toWhereClause(StringBuilder sql) {
        sql.append(" AND sc.height <= ? ");
        if (address != null) {
            sql.append(" AND sc.address = ? ");
        }
        if (transaction != null) {
            sql.append(" AND sc.transaction_id = ? ");
        }
        if (owner != null) {
            sql.append(" AND sc.owner = ? ");
        }
        if (StringUtils.isNotBlank(name)) {
            sql.append(" AND sc.name LIKE ? ");
        }
        if (StringUtils.isNotBlank(baseContract)) {
            sql.append(" AND sc.base_contract LIKE ? ");
        }
        if (timestamp != null) {
            sql.append(" AND sc.block_timestamp >= ? ");
        }
        if (status != null) {
            sql.append(" AND ss.status = ? ");
        }
        return sql;
    }

    public void setPreparedStatementParameters(PreparedStatement stmt) throws SQLException {
        setPreparedStatementParameters(stmt, true);
    }

    public void setPreparedStatementParameters(PreparedStatement stmt, boolean addLimits) throws SQLException {
        int i = 0;
        stmt.setInt(++i, height);
        if (address != null) {
            stmt.setLong(++i, address);
        }
        if (transaction != null) {
            stmt.setLong(++i, transaction);
        }
        if (owner != null) {
            stmt.setLong(++i, owner);
        }
        if (StringUtils.isNotBlank(name)) {
            stmt.setString(++i, DbUtils.escapeLikePattern(name));
        }
        if (StringUtils.isNotBlank(baseContract)) {
            stmt.setString(++i, DbUtils.escapeLikePattern(baseContract));
        }
        if (timestamp != null) {
            stmt.setInt(++i, Convert2.toEpochTime(timestamp));
        }
        if (status != null) {
            stmt.setString(++i, status);
        }
        if (addLimits) {
            DbUtils.setLimits(++i, stmt, paging);
        }
    }
}
