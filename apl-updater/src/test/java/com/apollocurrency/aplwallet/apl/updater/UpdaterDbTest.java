/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterRepository;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Platform;
import org.junit.Assert;
import org.junit.Test;

public class UpdaterDbTest {
       private static final DbManipulator manipulator = new DbManipulator();
    protected static final DataSource dataSource = manipulator.getDataSource();
    private UpdaterRepository repository = new UpdaterDbRepository(new MockUpdaterMediator());

    @Test
    public void testLoadUpdateTransaction() throws Exception {
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(Update.IMPORTANT, transaction.getType());
        Assert.assertEquals(104595, transaction.getHeight());
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getAppVersion(), new Version("1.0.8"));
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.X86);
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((UpdateAttachment) transaction.getAttachment()).getHash()), (
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
        Assert.assertEquals(Update.CRITICAL, transaction.getType());
        Assert.assertEquals(104671, transaction.getHeight());
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getAppVersion(), new Version("1.0.8"));
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getArchitecture(), Architecture.AMD64);
        Assert.assertEquals(((UpdateAttachment) transaction.getAttachment()).getPlatform(), Platform.LINUX);
        Assert.assertEquals(Convert.toHexString(((UpdateAttachment) transaction.getAttachment()).getHash()), ("a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4"));
        Assert.assertFalse(updateTransaction.isUpdated());
    }

    @Test
    public void testClearAndSaveUpdateTransaction() {
        repository.clearAndSave(new UpdateTransaction(new SimpleTransaction(-4081443370478530685L, null), false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(-4081443370478530685L, transaction.getId());
        Assert.assertEquals(Update.CRITICAL, transaction.getAttachment().getTransactionType());
        Assert.assertFalse(updateTransaction.isUpdated());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveTwoUpdateTransactions() {
        repository.save(new UpdateTransaction(new SimpleTransaction(-4081443370478530685L, null), false));
        UpdateTransaction last = repository.getLast();
    }
    private class MockUpdaterMediator extends UpdaterMediatorImpl {
        public MockUpdaterMediator() {
            super();
        }

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
                UpdateAttachment attachment = (UpdateAttachment) transactionType.parseAttachment(buffer);
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
        public TransactionalDataSource getDataSource() {
            return new TransactionalDataSource(null, null) {
                Connection connection;

                @Override
                public Connection getConnection() throws SQLException {
                    connection = dataSource.getConnection();
                    return connection;
                }

                @Override
                public void begin() {
                    try {
                        connection = dataSource.getConnection();
                        connection.setAutoCommit(false);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }

                @Override
                public void rollback() {
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
                public void commit() {
                    try {
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        };}



        }
    }
