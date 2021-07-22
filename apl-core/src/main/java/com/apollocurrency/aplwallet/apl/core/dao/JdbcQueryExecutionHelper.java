/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao;

import com.apollocurrency.aplwallet.apl.core.dao.exception.AplCoreDaoException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.enterprise.inject.Vetoed;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>Reduce amount of boilerplate code, when working with JDBC (db table querying/updating)</p>
 * <p>Should be used wherever possible to replace the following code:</p>
 * <pre>
 *         try (Connection con = dataSource.getConnection();
 *              PreparedStatement pstmt = con.prepareStatement("SELECT * FROM example_table WHERE id = ?")) {
 *              pstmt.setLong(1, id);
 *             try (ResultSet rs = pstmt.executeQuery()) {
 *                 if (rs.next()) {
 *                     return entityRowMapper.mapWithException(rs, null);
 *                 }
 *                 return null;
 *             }
 *         } catch (SQLException e) {
 *             throw new RuntimeException(e.toString(), e);
 *         }
 * </pre>
 * with the following code:
 * <pre>
 *     return queryExecutionHelper.executeQuery((con) -> {
 *             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM example_table WHERE id = ?");
 *              pstmt.setLong(1, id);
 *             return pstmt;
 *         });
 * </pre>
 *
 * @author Andrii Boiarskyi
 * @since 1.48.4
 */
@Vetoed
public class JdbcQueryExecutionHelper<T> {
    private static final int DEFAULT_MAX_ERROR_FETCH_LIMIT = 10;
    @Getter
    @Setter
    private volatile int errorFetchLimit = DEFAULT_MAX_ERROR_FETCH_LIMIT;
    private final DataSource dataSource;
    private final ThrowingFunction<ResultSet, T> mapper;

    public JdbcQueryExecutionHelper(@NonNull DataSource dataSource, @NonNull ThrowingFunction<ResultSet, T> mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    public Optional<T> executeQuery(@NonNull ThrowingFunction<Connection, @NonNull PreparedStatement> statementSupplier) {
        return executeInConnection((con) -> {
            PreparedStatement stmt = statementSupplier.apply(con);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    T value = mapper.apply(rs);
                    verifyNoMoreElements(rs);
                    return Optional.ofNullable(value);
                } else {
                    return Optional.empty();
                }
            }
        });
    }

    public int executeUpdate(@NonNull ThrowingFunction<Connection, PreparedStatement> statementSupplier) {
        return executeInConnection((con) -> {
            PreparedStatement stmt = statementSupplier.apply(con);
            return stmt.executeUpdate();
        });
    }

    public List<T> executeListQuery(@NonNull ThrowingFunction<Connection, PreparedStatement> statementSupplier) {
        return executeInConnection((con) -> {
            PreparedStatement stmt = statementSupplier.apply(con);
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    T value = mapper.apply(rs);
                    results.add(value);
                }
                return results;
            }
        });
    }

    private void verifyNoMoreElements(ResultSet rs) throws SQLException {
        List<T> elementsLeft = new ArrayList<>();
        int totalSelected = 0;
        while (rs.next()) {
            totalSelected++;
            if (elementsLeft.size() == errorFetchLimit) {
                continue;
            }
            T value = mapper.apply(rs);
            elementsLeft.add(value);
        }
        if (totalSelected > 0) {
            throw new AplCoreDaoException("Expected one element in the query result, got: " + (totalSelected + 1) + ", extra elements: " + elementsLeft);
        }
    }

    private <M> M executeInConnection(ThrowingFunction<Connection, M> executor) {
        try (Connection con = dataSource.getConnection()) {
            return executor.apply(con);
        } catch (SQLException e) {
            throw new AplCoreDaoException(e.toString(), e);
        }
    }

    public interface ThrowingFunction<V,S> {
        S apply(V v) throws SQLException;
    }
}
