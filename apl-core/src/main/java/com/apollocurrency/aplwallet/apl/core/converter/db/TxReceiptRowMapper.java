/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class TxReceiptRowMapper implements RowMapper<TxReceipt> {
    private final TransactionTypeFactory typeFactory;

    @Inject
    public TxReceiptRowMapper(TransactionTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public TxReceipt map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            return parseTxReceipt(rs);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private TxReceipt parseTxReceipt(ResultSet rs) throws AplException.NotValidException {
        try {
            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            long amountATM = rs.getLong("amount");
            long feeATM = rs.getLong("fee");
            byte version = rs.getByte("version");

            SignatureParser parser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            ByteBuffer signatureBuffer = ByteBuffer.wrap(rs.getBytes("signature"));
            Signature signature = parser.parse(signatureBuffer);

            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            long senderId = rs.getLong("sender_id");
            long recipientId = rs.getLong("recipient_id");
            byte[] attachmentBytes = rs.getBytes("attachment_bytes");
            int blockTimestamp = rs.getInt("block_timestamp");
            ByteBuffer buffer = null;
            if (attachmentBytes != null) {
                buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            short transactionIndex = rs.getShort("transaction_index");
            String payload = null;
            if (rs.getBoolean("has_message")) {
                TransactionType transactionType = typeFactory.findTransactionType(type, subtype);
                if (transactionType == null) {
                    throw new AplException.NotValidException("Wrong transaction type/subtype value, type=" + type + " subtype=" + subtype);
                }
                transactionType.parseAttachment(buffer);
                payload = Convert.toString(new MessageAppendix(buffer).getMessage());
            }

            TxReceipt transaction = new TxReceipt();
            transaction.setTransaction(Long.toUnsignedString(id));
            transaction.setSender(Convert2.defaultRsAccount(senderId));
            transaction.setRecipient(recipientId != 0 ? Convert2.defaultRsAccount(recipientId) : "0");
            transaction.setAmount(Long.toUnsignedString(amountATM));
            transaction.setFee(Long.toUnsignedString(feeATM));
            transaction.setTimestamp((long) timestamp);
            transaction.setHeight((long) height);
            transaction.setBlock(Long.toUnsignedString(blockId));
            transaction.setBlockTimestamp((long) blockTimestamp);
            transaction.setIndex((int) transactionIndex);
            transaction.setSignature(signature.getHexString());
            transaction.setPayload(payload);
            return transaction;

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
