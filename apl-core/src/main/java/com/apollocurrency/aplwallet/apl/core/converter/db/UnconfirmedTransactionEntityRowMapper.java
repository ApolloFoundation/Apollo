/*
 *  Copyright © 2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Singleton;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class UnconfirmedTransactionEntityRowMapper implements RowMapper<UnconfirmedTransactionEntity> {

    @Override
    public UnconfirmedTransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            return mapWithException(rs, ctx);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public UnconfirmedTransactionEntity mapWithException(ResultSet rs, StatementContext ctx) throws AplException.NotValidException {
        try {
            UnconfirmedTransactionEntity entity = UnconfirmedTransactionEntity.builder()
                    .id(rs.getLong("id"))
                    .transactionHeight(rs.getInt("transaction_height"))
                    .arrivalTimestamp(rs.getLong("arrival_timestamp"))
                    .feePerByte(rs.getLong("fee_per_byte"))
                    .expiration(rs.getInt("expiration"))
                    .transactionBytes(rs.getBytes("transaction_bytes"))
                    .prunableAttachmentJsonString(rs.getString("prunable_json"))
                    .build();
            entity.setDbId(rs.getLong("db_id"));
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
