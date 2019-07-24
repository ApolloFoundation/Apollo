/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.message;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PrunableMessageMapper extends DerivedEntityMapper<PrunableMessage> {
    public PrunableMessageMapper(KeyFactory<PrunableMessage> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PrunableMessage doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long senderId = rs.getLong("sender_id");
        long recipientId = rs.getLong("recipient_id");
        byte[] message = rs.getBytes("message");
        boolean messageIsText = rs.getBoolean("message_is_text");
        byte[] encryptedMessage = rs.getBytes("encrypted_message");
        EncryptedData encryptedData = EncryptedData.readEncryptedData(encryptedMessage);
        boolean encryptedMessageIsText = rs.getBoolean("encrypted_is_text");
        boolean isCompressed = rs.getBoolean("is_compressed");
        int blockTimestamp = rs.getInt("block_timestamp");
        int transactionTimestamp = rs.getInt("transaction_timestamp");
        return new PrunableMessage(null, null, id, senderId, recipientId, message, encryptedData, messageIsText, encryptedMessageIsText, isCompressed, transactionTimestamp, blockTimestamp);
    }
}
