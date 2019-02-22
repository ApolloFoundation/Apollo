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

import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.inject.Singleton;

@Singleton
public class TransactionDaoImpl implements TransactionDao {

    private final DatabaseManager databaseManager;
    private final BlockDao blockDao;

    @Inject
    public TransactionDaoImpl(BlockDao blockDao, DatabaseManager databaseManager) {
        Objects.requireNonNull(blockDao);
        this.blockDao = blockDao;
        this.databaseManager = databaseManager;
    }

    @Override
    public Transaction findTransaction(long transactionId) {
        return findTransaction(transactionId, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransaction(long transactionId, int height) {
        // Check the block cache
        synchronized (blockDao.getBlockCache()) {
            Transaction transaction = blockDao.getTransactionCache().get(transactionId);
            if (transaction != null) {
                return transaction.getHeight() <= height ? transaction : null;
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt("height") <= height) {
                    return loadTransaction(con, rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, id = " + transactionId + ", does not pass validation!", e);
        }
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        return findTransactionByFullHash(fullHash, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash, int height) {
        long transactionId = Convert.fullHashToId(fullHash);
        // Check the cache
        synchronized(blockDao.getBlockCache()) {
            Transaction transaction = blockDao.getTransactionCache().get(transactionId);
            if (transaction != null) {
                return (transaction.getHeight() <= height &&
                        Arrays.equals(transaction.getFullHash(), fullHash) ? transaction : null);
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && Arrays.equals(rs.getBytes("full_hash"), fullHash) && rs.getInt("height") <= height) {
                    return loadTransaction(con, rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, full_hash = " + Convert.toHexString(fullHash)
                    + ", does not pass validation!", e);
        }
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return hasTransaction(transactionId, Integer.MAX_VALUE);
    }

    @Override
    public boolean hasTransaction(long transactionId, int height) {
        // Check the block cache
        synchronized(blockDao.getBlockCache()) {
            Transaction transaction = blockDao.getTransactionCache().get(transactionId);
            if (transaction != null) {
                return (transaction.getHeight() <= height);
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT height FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash) {
        return Arrays.equals(fullHash, getFullHash(Convert.fullHashToId(fullHash)));
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash, int height) {
        long transactionId = Convert.fullHashToId(fullHash);
        // Check the block cache
        synchronized(blockDao.getBlockCache()) {
            Transaction transaction = blockDao.getTransactionCache().get(transactionId);
            if (transaction != null) {
                return (transaction.getHeight() <= height &&
                        Arrays.equals(transaction.getFullHash(), fullHash));
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT full_hash, height FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && Arrays.equals(rs.getBytes("full_hash"), fullHash) && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public byte[] getFullHash(long transactionId) {
        // Check the block cache
        synchronized(blockDao.getBlockCache()) {
            Transaction transaction = blockDao.getTransactionCache().get(transactionId);
            if (transaction != null) {
                return transaction.getFullHash();
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT full_hash FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getBytes("full_hash") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException {
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
            byte[] signature = rs.getBytes("signature");
            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            long senderId = rs.getLong("sender_id");
            byte[] attachmentBytes = rs.getBytes("attachment_bytes");
            int blockTimestamp = rs.getInt("block_timestamp");
            byte[] fullHash = rs.getBytes("full_hash");
            byte version = rs.getByte("version");
            short transactionIndex = rs.getShort("transaction_index");

            ByteBuffer buffer = null;
            if (attachmentBytes != null) {
                buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, null,
                    amountATM, feeATM, deadline, transactionType.parseAttachment(buffer))
                    .timestamp(timestamp)
                    .referencedTransactionFullHash(referencedTransactionFullHash)
                    .signature(signature)
                    .blockId(blockId)
                    .height(height)
                    .id(id)
                    .senderId(senderId)
                    .blockTimestamp(blockTimestamp)
                    .fullHash(fullHash)
                    .ecBlockHeight(ecBlockHeight)
                    .ecBlockId(ecBlockId)
                    .index(transactionIndex);
            if (transactionType.canHaveRecipient()) {
                long recipientId = rs.getLong("recipient_id");
                if (! rs.wasNull()) {
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
                builder.appendix(new PhasingAppendix(buffer));
            }
            if (rs.getBoolean("has_prunable_message")) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            if (rs.getBoolean("has_prunable_encrypted_message")) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }

            return builder.build();

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> findBlockTransactions(long blockId) {
        // Check the block cache
        synchronized(blockDao.getBlockCache()) {
            Block block = blockDao.getBlockCache().get(blockId);
            if (block != null) {
                return block.getTransactions();
            }
        }
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            return findBlockTransactions(con, blockId);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> findBlockTransactions(Connection con, long blockId) {
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE block_id = ? ORDER BY transaction_index")) {
            pstmt.setLong(1, blockId);
            pstmt.setFetchSize(50);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Transaction> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(loadTransaction(con, rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database for block_id = " + Long.toUnsignedString(blockId)
                    + " does not pass validation!", e);
        }
    }

    @Override
    public List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp) {
        List<PrunableTransaction> result = new ArrayList<>();
        try (PreparedStatement pstmt = con.prepareStatement("SELECT id, type, subtype, "
                + "has_prunable_attachment AS prunable_attachment, "
                + "has_prunable_message AS prunable_plain_message, "
                + "has_prunable_encrypted_message AS prunable_encrypted_message "
                + "FROM transaction WHERE (timestamp BETWEEN ? AND ?) AND "
                + "(has_prunable_attachment = TRUE OR has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE)")) {
            pstmt.setInt(1, minTimestamp);
            pstmt.setInt(2, maxTimestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    byte type = rs.getByte("type");
                    byte subtype = rs.getByte("subtype");
                    TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
                    result.add(new PrunableTransaction(id, transactionType,
                            rs.getBoolean("prunable_attachment"),
                            rs.getBoolean("prunable_plain_message"),
                            rs.getBoolean("prunable_encrypted_message")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public void saveTransactions(Connection con, List<Transaction> transactions) {
        try {
            short index = 0;
            for (Transaction transaction : transactions) {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, "
                        + "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
                        + "block_id, signature, timestamp, type, subtype, sender_id, attachment_bytes, "
                        + "block_timestamp, full_hash, version, has_message, has_encrypted_message, has_public_key_announcement, "
                        + "has_encrypttoself_message, phased, has_prunable_message, has_prunable_encrypted_message, "
                        + "has_prunable_attachment, ec_block_height, ec_block_id, transaction_index) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, transaction.getId());
                    pstmt.setShort(++i, transaction.getDeadline());
                    DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getRecipientId());
                    pstmt.setLong(++i, transaction.getAmountATM());
                    pstmt.setLong(++i, transaction.getFeeATM());
                    DbUtils.setBytes(pstmt, ++i, transaction.referencedTransactionFullHash());
                    pstmt.setInt(++i, transaction.getHeight());
                    pstmt.setLong(++i, transaction.getBlockId());
                    pstmt.setBytes(++i, transaction.getSignature());
                    pstmt.setInt(++i, transaction.getTimestamp());
                    pstmt.setByte(++i, transaction.getType().getType());
                    pstmt.setByte(++i, transaction.getType().getSubtype());
                    pstmt.setLong(++i, transaction.getSenderId());
                    int bytesLength = 0;
                    for (Appendix appendage : transaction.getAppendages()) {
                        bytesLength += appendage.getSize();
                    }
                    if (bytesLength == 0) {
                        pstmt.setNull(++i, Types.VARBINARY);
                    } else {
                        ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        for (Appendix appendage : transaction.getAppendages()) {
                            appendage.putBytes(buffer);
                        }
                        pstmt.setBytes(++i, buffer.array());
                    }
                    pstmt.setInt(++i, transaction.getBlockTimestamp());
                    pstmt.setBytes(++i, transaction.getFullHash());
                    pstmt.setByte(++i, transaction.getVersion());
                    pstmt.setBoolean(++i, transaction.getMessage() != null);
                    pstmt.setBoolean(++i, transaction.getEncryptedMessage() != null);
                    pstmt.setBoolean(++i, transaction.getPublicKeyAnnouncement() != null);
                    pstmt.setBoolean(++i, transaction.getEncryptToSelfMessage() != null);
                    pstmt.setBoolean(++i, transaction.getPhasing() != null);
                    pstmt.setBoolean(++i, transaction.hasPrunablePlainMessage());
                    pstmt.setBoolean(++i, transaction.hasPrunableEncryptedMessage());
                    pstmt.setBoolean(++i, transaction.getAttachment() instanceof Prunable);
                    pstmt.setInt(++i, transaction.getECBlockHeight());
                    DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getECBlockId());
                    pstmt.setShort(++i, index++);
                    pstmt.executeUpdate();
                }
                if (transaction.referencedTransactionFullHash() != null) {
                    try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO referenced_transaction "
                         + "(transaction_id, referenced_transaction_id) VALUES (?, ?)")) {
                        pstmt.setLong(1, transaction.getId());
                        pstmt.setLong(2, Convert.fullHashToId(transaction.referencedTransactionFullHash()));
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getTransactionCount() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction");
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getAllTransactions() {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getTransactions(
            long accountId, int numberOfConfirmations, byte type, byte subtype,
            int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
            int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
            int height, int prunableExpiration) {
        if (phasedOnly && nonPhasedOnly) {
            throw new IllegalArgumentException("At least one of phasedOnly or nonPhasedOnly must be false");
        }

        StringBuilder buf = new StringBuilder();
        buf.append("SELECT transaction.* FROM transaction ");
        if (executedOnly && !nonPhasedOnly) {
            buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
        }
        buf.append("WHERE recipient_id = ? AND sender_id <> ? ");
        if (blockTimestamp > 0) {
            buf.append("AND block_timestamp >= ? ");
        }
        if (!includePrivate && TransactionType.findTransactionType(type, subtype) == Payment.PRIVATE) {
            throw new RuntimeException("None of private transactions should be retrieved!");
        }
        if (type >= 0) {
            buf.append("AND type = ? ");
            if (subtype >= 0) {
                buf.append("AND subtype = ? ");
            }
        }
        if (!includePrivate) {
            buf.append("AND (type <> ? ");
            buf.append("OR subtype <> ? ) ");
        }
        if (height < Integer.MAX_VALUE) {
            buf.append("AND transaction.height <= ? ");
        }
        if (withMessage) {
            buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE ");
            buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
        }
        if (phasedOnly) {
            buf.append("AND phased = TRUE ");
        } else if (nonPhasedOnly) {
            buf.append("AND phased = FALSE ");
        }
        if (executedOnly && !nonPhasedOnly) {
            buf.append("AND (phased = FALSE OR approved = TRUE) ");
        }
        buf.append("UNION ALL SELECT transaction.* FROM transaction ");
        if (executedOnly && !nonPhasedOnly) {
            buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
        }
        buf.append("WHERE sender_id = ? ");
        if (blockTimestamp > 0) {
            buf.append("AND block_timestamp >= ? ");
        }
        if (type >= 0) {
            buf.append("AND type = ? ");
            if (subtype >= 0) {
                buf.append("AND subtype = ? ");
            }
        }
        if (!includePrivate) {
            buf.append("AND (type <> ? ");
            buf.append("OR subtype <> ? ) ");
        }
        if (height < Integer.MAX_VALUE) {
            buf.append("AND transaction.height <= ? ");
        }
        if (withMessage) {
            buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE OR has_encrypttoself_message = TRUE ");
            buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
        }
        if (phasedOnly) {
            buf.append("AND phased = TRUE ");
        } else if (nonPhasedOnly) {
            buf.append("AND phased = FALSE ");
        }
        if (executedOnly && !nonPhasedOnly) {
            buf.append("AND (phased = FALSE OR approved = TRUE) ");
        }

        buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
        buf.append(DbUtils.limitsClause(from, to));
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(buf.toString());
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (!includePrivate) {
                pstmt.setByte(++i, Payment.PRIVATE.getType());
                pstmt.setByte(++i, Payment.PRIVATE.getSubtype());
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (!includePrivate) {
                pstmt.setByte(++i, Payment.PRIVATE.getType());
                pstmt.setByte(++i, Payment.PRIVATE.getSubtype());
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to) {
        StringBuilder sqlQuery = new StringBuilder("SELECT * FROM transaction WHERE (type <> ? OR subtype <> ?) ");
        if (type >= 0) {
            sqlQuery.append("AND type = ? ");
            if (subtype >= 0) {
                sqlQuery.append("AND subtype = ? ");
            }
        }
        sqlQuery.append("ORDER BY block_timestamp DESC, transaction_index DESC ");
        sqlQuery.append(DbUtils.limitsClause(from, to));
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement statement = con.prepareStatement(sqlQuery.toString());
            int i = 0;
            statement.setByte(++i, Payment.PRIVATE.getType());
            statement.setByte(++i, Payment.PRIVATE.getSubtype());
            if (type >= 0) {
                statement.setByte(++i, type);
                if (subtype >= 0) {
                    statement.setByte(++i, subtype);
                }
            }
            DbUtils.setLimits(++i, statement, from, to);
            return getTransactions(con, statement);
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getTransactionCount(long accountId, byte type, byte subtype) {
        StringBuilder sqlQuery = new StringBuilder("SELECT COUNT(*) FROM transaction WHERE (type <> ? OR subtype <> ?) AND (sender_id = ? OR recipient_id = ?) ");
        if (type >= 0) {
            sqlQuery.append("AND type = ? ");
            if (subtype >= 0) {
                sqlQuery.append("AND subtype = ? ");
            }
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(sqlQuery.toString())) {
            int i = 0;
            statement.setByte(++i, Payment.PRIVATE.getType());
            statement.setByte(++i, Payment.PRIVATE.getSubtype());
            statement.setLong(++i, accountId);
            statement.setLong(++i, accountId);
            if (type >= 0) {
                statement.setByte(++i, type);
                if (subtype >= 0) {
                    statement.setByte(++i, subtype);
                }
            }
            try(ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, this::loadTransaction);
    }

    @Override
    public DbIterator<Transaction> getReferencingTransactions(long transactionId, int from, int to) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, referenced_transaction "
                    + "WHERE referenced_transaction.referenced_transaction_id = ? "
                    + "AND referenced_transaction.transaction_id = transaction.id "
                    + "ORDER BY transaction.block_timestamp DESC, transaction.transaction_index DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, transactionId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

}
