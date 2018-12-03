/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.ConnectionProvider;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.updater.SimpleTransaction;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;

public class UpdaterDbTest extends DbIntegrationTest {
    private UpdaterRepository repository = new UpdaterDbRepository(new MockUpdaterMediator());

    @Test
    public void testLoadUpdateTransaction() throws Exception {
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(TransactionType.Update.IMPORTANT, transaction.getType());
        Assert.assertEquals(104595, transaction.getHeight());
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getAppVersion(), Version.from("1.0.8"));
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.X86);
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((Attachment.UpdateAttachment) transaction.getAttachment()).getHash()), (
                "a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
        Assert.assertTrue(updateTransaction.isUpdated());
    }

    @Test
    public void testClear() throws Exception {
        int actual = repository.clear();
        Assert.assertEquals(1, actual);
        UpdateTransaction tr = repository.getLast();
        Assert.assertNull(tr);
    }

    @Test
    public void testSaveUpdateTransaction() throws Exception {
        repository.clear();
        repository.save(new UpdateTransaction(new SimpleTransaction(-4081443370478530685L, null), false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(TransactionType.Update.CRITICAL, transaction.getType());
        Assert.assertEquals(104671, transaction.getHeight());
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getAppVersion(), Version.from("1.0.8"));
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.AMD64);
        Assert.assertEquals(((Attachment.UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((Attachment.UpdateAttachment) transaction.getAttachment()).getHash()), ("a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
        Assert.assertFalse(updateTransaction.isUpdated());
    }

    @Test
    public void testClearAndSaveUpdateTransaction() {
        repository.clearAndSave(new UpdateTransaction(new SimpleTransaction(-4081443370478530685L, null), false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(-4081443370478530685L, transaction.getId());
        Assert.assertEquals(TransactionType.Update.CRITICAL, transaction.getAttachment().getTransactionType());
        Assert.assertFalse(updateTransaction.isUpdated());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveTwoUpdateTransactions() {
        repository.save(new UpdateTransaction(new SimpleTransaction(-4081443370478530685L, null), false));
        UpdateTransaction last = repository.getLast();
    }
    private class MockUpdaterMediator extends UpdaterMediatorImpl {
        @Override
        public Transaction loadTransaction(Connection connection, ResultSet rs) throws AplException.NotValidException {
            try {
                int height = rs.getInt("height");
                long id = rs.getLong("id");
                byte type = rs.getByte("type");
                byte subType = rs.getByte("subtype");
                byte[] attachmentBytes = rs.getBytes("attachment_bytes");
                TransactionType transactionType = TransactionType.findTransactionType(type, subType);

                ByteBuffer buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transactionType.parseAttachment(buffer);
                SimpleTransaction simpleTransaction = new SimpleTransaction(id, transactionType, height);
                simpleTransaction.setAttachment(attachment);
                return simpleTransaction;
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public ConnectionProvider getConnectionProvider() {
            return new ConnectionProvider() {
                @Override
                public Connection getConnection() throws SQLException {
                    return db.getConnection();
                }

                @Override
                public Connection beginTransaction() {
                    try {
                        Connection connection = db.getConnection();
                        connection.setAutoCommit(false);
                        return connection;
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }

                @Override
                public void rollbackTransaction(Connection connection) {
                    if (connection != null) {
                        try {
                            connection.rollback();
                        }
                        catch (SQLException e) {
                            throw new RuntimeException(e.toString(), e);
                        }
                    }
                }

                @Override
                public void commitTransaction(Connection connection) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
            @Override
            public void endTransaction (Connection connection){
                try {
                    if (connection != null) {
                        connection.close();
                    }
                }
                catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
            @Override
            public boolean isInTransaction(Connection connection) {
                return false;
            }
        };}



        }
    }
