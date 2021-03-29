/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @deprecated use #TransactionEntityRowMapper and #TransactionEntityToModelConverter
 */
@Deprecated(forRemoval = true)
@Singleton
public class TransactionRowMapper implements RowMapper<Transaction> {
    private final TransactionTypeFactory factory;
    private final TransactionBuilderFactory transactionBuilderFactory;

    @Inject
    public TransactionRowMapper(TransactionTypeFactory factory, TransactionBuilderFactory transactionBuilderFactory) {
        this.factory = factory;
        this.transactionBuilderFactory = transactionBuilderFactory;
    }


    @Override
    public Transaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            return mapWithException(rs, ctx);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Transaction mapWithException(ResultSet rs, StatementContext ctx) throws AplException.NotValidException {
        try {
            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            short deadline = rs.getShort("deadline");
            long amountATM = rs.getLong("amount");
            long feeATM = rs.getLong("fee");
            byte[] referencedTransactionFullHash = rs.getBytes("referenced_transaction_full_hash");
            int ecBlockHeight = rs.getInt("ec_block_height");
            long ecBlockId = rs.getLong("ec_block_id");
            byte version = rs.getByte("version");

            SignatureParser parser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            ByteBuffer signatureBuffer = ByteBuffer.wrap(rs.getBytes("signature"));
            Signature signature = parser.parse(signatureBuffer);

            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            long senderId = rs.getLong("sender_id");
            byte[] attachmentBytes = rs.getBytes("attachment_bytes");
            int blockTimestamp = rs.getInt("block_timestamp");
            byte[] fullHash = rs.getBytes("full_hash");

            short transactionIndex = rs.getShort("transaction_index");
            long dbId = rs.getLong("db_id");
            byte[] senderPublicKey = rs.getBytes("sender_public_key"); // will be null for already registered public keys, which exist in public_key table

            ByteBuffer buffer = null;
            if (attachmentBytes != null) {
                buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            TransactionType transactionType = factory.findTransactionType(type, subtype);
            Transaction.Builder builder = transactionBuilderFactory.newUnsignedTransactionBuilder(version, senderPublicKey,
                amountATM, feeATM, deadline, transactionType != null ? transactionType.parseAttachment(buffer) : null, timestamp)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .blockId(blockId)
                .height(height)
                .id(id)
                .senderId(senderId)
                .blockTimestamp(blockTimestamp)
                .fullHash(fullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId)
                .index(transactionIndex);

            if (transactionType != null && transactionType.canHaveRecipient()) {
                long recipientId = rs.getLong("recipient_id");
                if (!rs.wasNull()) {
                    builder.recipientId(recipientId);
                }
            }
            if (rs.getBoolean("has_message")) {
                builder.appendix(new MessageAppendix(buffer));
            }
            if (rs.getBoolean("has_encrypted_message")) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            if (rs.getBoolean("has_public_key_announcement")) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            if (rs.getBoolean("has_encrypttoself_message")) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            if (rs.getBoolean("phased")) {
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            if (rs.getBoolean("has_prunable_message")) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            if (rs.getBoolean("has_prunable_encrypted_message")) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }

            builder.signature(signature);
            return builder.build();

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
