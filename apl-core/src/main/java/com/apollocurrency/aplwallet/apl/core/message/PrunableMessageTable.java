/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.message;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class PrunableMessageTable extends PrunableDbTable<PrunableMessage> {
    private static final String TABLE_NAME = "prunable_message";
    private static final LongKeyFactory<PrunableMessage> KEY_FACTORY = new LongKeyFactory<PrunableMessage>("id") {

        @Override
        public DbKey newKey(PrunableMessage prunableMessage) {
            if (prunableMessage.getDbKey() == null) {
                prunableMessage.setDbKey(new LongKey(prunableMessage.getId()));
            }
            return prunableMessage.getDbKey();
        }

    };
    private static final PrunableMessageMapper MAPPER = new PrunableMessageMapper(KEY_FACTORY);


    public PrunableMessageTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }

    @Override
        public boolean isScanSafe() {
            return false; // all messages cannot be recovered from transaction attachment data, so that should not be reverted without block popOff
        }

        @Override
        public PrunableMessage load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return MAPPER.map(rs, null);
        }

        @Override
        public void save(Connection con, PrunableMessage prunableMessage) throws SQLException {
            if (prunableMessage.getMessage() == null && prunableMessage.getEncryptedData() == null) {
                throw new IllegalStateException("Prunable message not fully initialized");
            }
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO prunable_message (id, sender_id, recipient_id, "
                    + "message, encrypted_message, message_is_text, encrypted_is_text, is_compressed, block_timestamp, transaction_timestamp, height) "
                    + "KEY (id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, prunableMessage.getId());
                pstmt.setLong(++i, prunableMessage.getSenderId());
                DbUtils.setLongZeroToNull(pstmt, ++i, prunableMessage.getRecipientId());
                DbUtils.setBytes(pstmt, ++i, prunableMessage.getMessage());
                DbUtils.setBytes(pstmt, ++i, prunableMessage.getEncryptedData() == null ? null : prunableMessage.getEncryptedData().getBytes());
                pstmt.setBoolean(++i, prunableMessage.messageIsText());
                pstmt.setBoolean(++i, prunableMessage.encryptedMessageIsText());
                pstmt.setBoolean(++i, prunableMessage.isCompressed());
                pstmt.setInt(++i, prunableMessage.getBlockTimestamp());
                pstmt.setInt(++i, prunableMessage.getTransactionTimestamp());
                pstmt.setInt(++i, prunableMessage.getHeight());
                pstmt.executeUpdate();
            }
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, db_id DESC ";
    }
}
