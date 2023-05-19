/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao;

import com.apollocurrency.aplwallet.apl.core.dao.exception.AplCoreDaoException;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("slow")
class JdbcQueryExecutionHelperTest extends DbContainerRootUserTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);

    TransactionTestData td = new TransactionTestData();
    private JdbcQueryExecutionHelper<Long> txIdQueryHelper;

    @BeforeEach
    void setUp() {
        txIdQueryHelper = new JdbcQueryExecutionHelper<>(dbExtension.getDatabaseManager().getDataSource(), (rs) -> rs.getLong("id"));
    }

    @Test
    void executeQuery_Ok() {
        Optional<Long> tx4IdOptional = txIdQueryHelper.executeQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("Select * from transaction where db_id = ?");
            pstmt.setLong(1, td.DB_ID_4);
            return pstmt;
        });

        assertTrue(tx4IdOptional.isPresent(), "Id for the transaction specified by the DB_ID=" + td.DB_ID_4 + " should be present");
        assertEquals(tx4IdOptional.get(), td.TRANSACTION_4.getId());
    }

    @Test
    void executeQuery_noValue() {
        Optional<Long> tx4IdOptional = txIdQueryHelper.executeQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("Select * from transaction where db_id = ?");
            pstmt.setLong(1, Long.MIN_VALUE);
            return pstmt;
        });

        assertTrue(tx4IdOptional.isEmpty(), "Id for the transaction specified by the DB_ID=" + Long.MIN_VALUE + " should NOT be present in the db");
    }

    @Test
    void executeQuery_manyValuesExceedingErrorFetchLimit() {
        txIdQueryHelper.setErrorFetchLimit(1);

        AplCoreDaoException exception = assertThrows(AplCoreDaoException.class, () -> txIdQueryHelper.executeQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("Select * from transaction where db_id > ? and db_id < ?");
            pstmt.setLong(1, Long.MIN_VALUE);
            pstmt.setLong(2, Long.MAX_VALUE);
            return pstmt;
        }));

        assertEquals("Expected one element in the query result, got: 15, extra elements: [" + td.TRANSACTION_1.getId() + "]", exception.getMessage());
    }

    @Test
    void executeQuery_manyValuesNotExceedingErrorFetchLimit() {
        txIdQueryHelper.setErrorFetchLimit(2);
        assertEquals(2, txIdQueryHelper.getErrorFetchLimit());

        AplCoreDaoException exception = assertThrows(AplCoreDaoException.class, () -> txIdQueryHelper.executeQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("Select * from transaction where db_id > ? and db_id < ?");
            pstmt.setLong(1, td.DB_ID_1);
            pstmt.setLong(2, td.DB_ID_4);
            return pstmt;
        }));

        assertEquals("Expected one element in the query result, got: 2, extra elements: [" + td.TRANSACTION_3.getId() + "]", exception.getMessage());
    }

    @Test
    void executeUpdate() {
        int deleted = txIdQueryHelper.executeUpdate((con) -> {
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM transaction where db_id < ?");
            pstmt.setLong(1, td.DB_ID_3);
            return pstmt;
        });

        assertEquals(3, deleted);
        List<Long> deletedIds = txIdQueryHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction where db_id < ?");
            pstmt.setLong(1, td.DB_ID_3);
            return pstmt;
        });
        assertEquals(List.of(), deletedIds);
    }

    @Test
    void executeListQuery() {
        List<Long> ids = txIdQueryHelper.executeListQuery((con) -> {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction where db_id > ? and db_id < ?");
            pstmt.setLong(1, td.DB_ID_3);
            pstmt.setLong(2, td.DB_ID_6);
            return pstmt;
        });

        assertEquals(List.of(td.TRANSACTION_4.getId(), td.TRANSACTION_5.getId()), ids);
    }

    @Test
    void interactWithNullParameters() {
        assertThrows(NullPointerException.class, () -> new JdbcQueryExecutionHelper<>(null, null));

        assertThrows(NullPointerException.class, () -> txIdQueryHelper.executeQuery(null));
        assertThrows(NullPointerException.class, () -> txIdQueryHelper.executeListQuery(null));
        assertThrows(NullPointerException.class, () -> txIdQueryHelper.executeUpdate(null));
    }

}