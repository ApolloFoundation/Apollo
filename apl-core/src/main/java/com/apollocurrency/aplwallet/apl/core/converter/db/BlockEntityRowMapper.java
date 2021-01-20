/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Singleton;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class BlockEntityRowMapper implements RowMapper<BlockEntity> {

    @Override
    public BlockEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            return mapWithException(rs, ctx);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public BlockEntity mapWithException(ResultSet rs, StatementContext ctx) throws AplException.NotValidException {
        try {
            long nextBlockId = rs.getLong("next_block_id");
            if (nextBlockId == 0 && !rs.wasNull()) {
                throw new AplException.NotValidException("Attempting to load invalid block");
            }

            BlockEntity entity = BlockEntity.builder()
                .version(rs.getInt("version"))
                .timestamp(rs.getInt("timestamp"))
                .previousBlockId(rs.getLong("previous_block_id"))
                .totalAmountATM(rs.getLong("total_amount"))
                .totalFeeATM(rs.getLong("total_fee"))
                .payloadLength(rs.getInt("payload_length"))
                .generatorId(rs.getLong("generator_id"))
                .previousBlockHash(rs.getBytes("previous_block_hash"))
                .cumulativeDifficulty(new BigInteger(rs.getBytes("cumulative_difficulty")))
                .baseTarget(rs.getLong("base_target"))
                .nextBlockId(nextBlockId)
                .height(rs.getInt("height"))
                .generationSignature(rs.getBytes("generation_signature"))
                .blockSignature(rs.getBytes("block_signature"))
                .payloadHash(rs.getBytes("payload_hash"))
                .id(rs.getLong("id"))
                .timeout(rs.getInt("timeout"))
                .dbId(rs.getLong("db_id"))
                .build();

            return entity;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
