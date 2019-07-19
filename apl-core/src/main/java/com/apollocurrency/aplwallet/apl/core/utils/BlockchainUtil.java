/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.TransactionRowMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BlockchainUtil {
    private static final TransactionRowMapper TX_MAPPER = new TransactionRowMapper();

    public static List<Transaction> loadTransactions(PreparedStatement pstmt) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(TX_MAPPER.map(rs, null));
            }
        }
        return result;
    }
}
