/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.converter.PrunableMessageMapper;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO prunable_message (id, sender_id, recipient_id, "
                + "message, encrypted_message, message_is_text, encrypted_is_text, is_compressed, block_timestamp, transaction_timestamp, height) "
                + "KEY (id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        ) {
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

    public List<PrunableMessage> getPrunableMessages(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = getDatabaseManager().getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM prunable_message WHERE sender_id = ?"
                + " UNION ALL SELECT * FROM prunable_message WHERE recipient_id = ? AND sender_id <> ? ORDER BY block_timestamp DESC, db_id DESC "
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return CollectionUtil.toList(getManyBy(con, pstmt, false));
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<PrunableMessage> getPrunableMessages(long accountId, long otherAccountId, int from, int to) {
        Connection con = null;
        try {
            con = getDatabaseManager().getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM prunable_message WHERE sender_id = ? AND recipient_id = ? "
                + "UNION ALL SELECT * FROM prunable_message WHERE sender_id = ? AND recipient_id = ? AND sender_id <> recipient_id "
                + "ORDER BY block_timestamp DESC, db_id DESC "
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, otherAccountId);
            pstmt.setLong(++i, otherAccountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return CollectionUtil.toList(getManyBy(con, pstmt, false));
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public boolean isPruned(long transactionId, boolean hasPrunablePlainMessage, boolean hasPrunableEncryptedMessage) {
        if (!hasPrunablePlainMessage && !hasPrunableEncryptedMessage) {
            return false;
        }
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT message, encrypted_message FROM prunable_message WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return !rs.next()
                    || (hasPrunablePlainMessage && rs.getBytes("message") == null)
                    || (hasPrunableEncryptedMessage && rs.getBytes("encrypted_message") == null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY block_timestamp DESC, db_id DESC ";
    }

    public PrunableMessage get(long transactionId) {
        return get(KEY_FACTORY.newKey(transactionId));
    }
}
