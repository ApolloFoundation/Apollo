/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import jakarta.inject.Singleton;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class TransactionEntityRowMapper implements RowMapper<TransactionEntity> {

    @Override
    public TransactionEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return mapWithException(rs, ctx);
    }

    public TransactionEntity mapWithException(ResultSet rs, StatementContext ctx)  {
        try {
            TransactionEntity entity = TransactionEntity.builder()
                    .version(rs.getByte("version"))
                    .type(rs.getByte("type"))
                    .subtype(rs.getByte("subtype"))
                    .id(rs.getLong("id"))
                .timestamp(rs.getInt("timestamp"))
                .deadline(rs.getShort("deadline"))
                .amountATM(rs.getLong("amount"))
                .feeATM(rs.getLong("fee"))
                .referencedTransactionFullHash(rs.getBytes("referenced_transaction_full_hash"))
                .ecBlockHeight(rs.getInt("ec_block_height"))
                .ecBlockId(rs.getLong("ec_block_id"))
                .signatureBytes(rs.getBytes("signature"))
                .blockId(rs.getLong("block_id"))
                .height(rs.getInt("height"))
                .senderId(rs.getLong("sender_id"))
                .recipientId(rs.getLong("recipient_id"))
                .attachmentBytes(rs.getBytes("attachment_bytes"))
                .blockTimestamp(rs.getInt("block_timestamp"))
                .fullHash(rs.getBytes("full_hash"))
                .index(rs.getShort("transaction_index"))
                .dbId(rs.getLong("db_id"))
                .senderPublicKey(rs.getBytes("sender_public_key"))
                .hasMessage(rs.getBoolean("has_message"))
                .hasEncryptedMessage(rs.getBoolean("has_encrypted_message"))
                .hasPublicKeyAnnouncement(rs.getBoolean("has_public_key_announcement"))
                .hasEncryptToSelfMessage(rs.getBoolean("has_encrypttoself_message"))
                .phased(rs.getBoolean("phased"))
                .hasPrunableMessage(rs.getBoolean("has_prunable_message"))
                .hasPrunableEencryptedMessage(rs.getBoolean("has_prunable_encrypted_message"))
                .hasPrunableAttachment(rs.getBoolean("has_prunable_attachment"))
                .errorMessage(rs.getString("error_message"))
                .build();

            return entity;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
