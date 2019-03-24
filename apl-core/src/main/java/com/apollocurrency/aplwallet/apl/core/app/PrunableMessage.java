/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.PrunableDbTable;


public final class PrunableMessage {

    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();
    private static LongKeyFactory<UnconfirmedTransaction> keyFactory = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();

    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    private static final LongKeyFactory<PrunableMessage> prunableMessageKeyFactory = new LongKeyFactory<PrunableMessage>("id") {

        @Override
        public DbKey newKey(PrunableMessage prunableMessage) {
            return prunableMessage.dbKey;
        }

    };

    private static final PrunableDbTable<PrunableMessage> prunableMessageTable = new PrunableDbTable<PrunableMessage>("prunable_message", prunableMessageKeyFactory) {

        @Override
        protected PrunableMessage load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PrunableMessage(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PrunableMessage prunableMessage) throws SQLException {
            prunableMessage.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, db_id DESC ";
        }

    };

    public static int getCount() {
        return prunableMessageTable.getCount();
    }

    public static DbIterator<PrunableMessage> getAll(int from, int to) {
        return prunableMessageTable.getAll(from, to);
    }

    public static PrunableMessage getPrunableMessage(long transactionId) {
        return prunableMessageTable.get(prunableMessageKeyFactory.newKey(transactionId));
    }

    public static DbIterator<PrunableMessage> getPrunableMessages(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM prunable_message WHERE sender_id = ?"
                    + " UNION ALL SELECT * FROM prunable_message WHERE recipient_id = ? AND sender_id <> ? ORDER BY block_timestamp DESC, db_id DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return prunableMessageTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<PrunableMessage> getPrunableMessages(long accountId, long otherAccountId, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
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
            return prunableMessageTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final long senderId;
    private final long recipientId;
    private byte[] message;
    private EncryptedData encryptedData;
    private boolean messageIsText;
    private boolean encryptedMessageIsText;
    private boolean isCompressed;
    private final int transactionTimestamp;
    private final int blockTimestamp;
    private final int height;

    private PrunableMessage(Transaction transaction, int blockTimestamp, int height) {
        this.id = transaction.getId();
        this.dbKey = prunableMessageKeyFactory.newKey(this.id);
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.blockTimestamp = blockTimestamp;
        this.height = height;
        this.transactionTimestamp = transaction.getTimestamp();
    }

    private void setPlain(PrunablePlainMessageAppendix appendix) {
        this.message = appendix.getMessage();
        this.messageIsText = appendix.isText();
    }

    private void setEncrypted(PrunableEncryptedMessageAppendix appendix) {
        this.encryptedData = appendix.getEncryptedData();
        this.encryptedMessageIsText = appendix.isText();
        this.isCompressed = appendix.isCompressed();
    }

    private PrunableMessage(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.message = rs.getBytes("message");
        if (this.message != null) {
            this.messageIsText = rs.getBoolean("message_is_text");
        }
        byte[] encryptedMessage = rs.getBytes("encrypted_message");
        if (encryptedMessage != null) {
            this.encryptedData = EncryptedData.readEncryptedData(encryptedMessage);
            this.encryptedMessageIsText = rs.getBoolean("encrypted_is_text");
            this.isCompressed = rs.getBoolean("is_compressed");
        }
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
        this.height = rs.getInt("height");
    }

    private PrunableMessage(long id, DbKey dbKey, long senderId, long recipientId, int transactionTimestamp, int blockTimestamp, int height) {
        this.id = id;
        this.dbKey = dbKey;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.transactionTimestamp = transactionTimestamp;
        this.blockTimestamp = blockTimestamp;
        this.height = height;
    }

    private void save(Connection con) throws SQLException {
        if (message == null && encryptedData == null) {
            throw new IllegalStateException("Prunable message not fully initialized");
        }
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO prunable_message (id, sender_id, recipient_id, "
                + "message, encrypted_message, message_is_text, encrypted_is_text, is_compressed, block_timestamp, transaction_timestamp, height) "
                + "KEY (id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.senderId);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.recipientId);
            DbUtils.setBytes(pstmt, ++i, this.message);
            DbUtils.setBytes(pstmt, ++i, this.encryptedData == null ? null : this.encryptedData.getBytes());
            pstmt.setBoolean(++i, this.messageIsText);
            pstmt.setBoolean(++i, this.encryptedMessageIsText);
            pstmt.setBoolean(++i, this.isCompressed);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.setInt(++i, this.transactionTimestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public byte[] getMessage() {
        return message;
    }

    public EncryptedData getEncryptedData() {
        return encryptedData;
    }

    public boolean messageIsText() {
        return messageIsText;
    }

    public boolean encryptedMessageIsText() {
        return encryptedMessageIsText;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public long getId() {
        return id;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public int getHeight() {
        return height;
    }

    public byte[] decrypt(String secretPhrase) {
        return decryptUsingKeySeed(Crypto.getKeySeed(secretPhrase));
    }

    public byte[] decryptUsingSharedKey(byte[] sharedKey) {
        if (encryptedData == null) {
            return null;
        }
        byte[] data = Crypto.aesDecrypt(encryptedData.getData(), sharedKey);
        if (isCompressed) {
            data = Convert.uncompress(data);
        }
        return data;
    }

    public byte[] decryptUsingKeySeed(byte[] keySeed) {
        if (encryptedData == null) {
            return null;
        }
        byte[] publicKey = senderId == Account.getId(Crypto.getPublicKey(keySeed))
                ? Account.getPublicKey(recipientId) : Account.getPublicKey(senderId);
        return Account.decryptFrom(publicKey, encryptedData, keySeed, isCompressed);
    }

    public static void add(TransactionImpl transaction, PrunablePlainMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

    public static void add(TransactionImpl transaction, PrunablePlainMessageAppendix appendix, int blockTimestamp, int height) {
        if (appendix.getMessage() != null) {
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            PrunableMessage prunableMessage = prunableMessageTable.get(dbKey);
            if (prunableMessage == null) {
                prunableMessage = new PrunableMessage(transaction, blockTimestamp, height);
            } else if (prunableMessage.height != height) {
                throw new RuntimeException("Attempt to modify prunable message from height " + prunableMessage.height + " at height " + height);
            }
            if (prunableMessage.getMessage() == null) {
                prunableMessage.setPlain(appendix);
                prunableMessageTable.insert(prunableMessage);
            }
        }
    }

    public static void add(TransactionImpl transaction, PrunableEncryptedMessageAppendix appendix) {
        add(transaction, appendix, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

    public static void add(TransactionImpl transaction, PrunableEncryptedMessageAppendix appendix, int blockTimestamp, int height) {
        if (appendix.getEncryptedData() != null) {
            DbKey dbKey = keyFactory.newKey(transaction.getId());
                PrunableMessage prunableMessage = prunableMessageTable.get(dbKey);
            if (prunableMessage == null) {
                prunableMessage = new PrunableMessage(transaction, blockTimestamp, height);
            } else if (prunableMessage.height != height) {
                throw new RuntimeException("Attempt to modify prunable message from height " + prunableMessage.height + " at height " + height);
            }
            if (prunableMessage.getEncryptedData() == null) {
                prunableMessage.setEncrypted(appendix);
                prunableMessageTable.insert(prunableMessage);
            }
        }
    }

    static boolean isPruned(long transactionId, boolean hasPrunablePlainMessage, boolean hasPrunableEncryptedMessage) {
        if (!hasPrunablePlainMessage && !hasPrunableEncryptedMessage) {
            return false;
        }
        try (Connection con = lookupDataSource().getConnection();
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

}
