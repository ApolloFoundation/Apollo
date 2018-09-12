/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.ConnectionProvider;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterDbRepository;
import com.apollocurrency.aplwallet.apl.updater.repository.UpdaterRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Db.class, TransactionalDb.class, Constants.class, Apl.class, BlockchainImpl.class, Logger.class})
@SuppressStaticInitializationFor({
        "com.apollocurrency.aplwallet.apl.Db", "com.apollocurrency.aplwallet.apl.util.Logger", "com.apollocurrency.aplwallet.apl.db.TransactionalDb", "com.apollocurrency" +
        ".aplwallet.apl.Constants", "com" +
        ".apollocurrency" +
        ".aplwallet.apl.Apl"})
public class UpdaterDbTest {
    private EmbeddedDatabase db;
    private UpdaterRepository repository = new UpdaterDbRepository(new MockUpdaterMediator());

    @Before
    public void setUp() throws Exception {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("db/schema.sql")
                .addScript("db/data.sql")
                .build();

    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

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
        repository.save(new UpdateTransaction(new IdTransaction(-4081443370478530685L), false));
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
        repository.clearAndSave(new UpdateTransaction(new IdTransaction(-4081443370478530685L), false));
        UpdateTransaction updateTransaction = repository.getLast();
        Transaction transaction = updateTransaction.getTransaction();
        Assert.assertEquals(-4081443370478530685L, transaction.getId());
        Assert.assertEquals(TransactionType.Update.CRITICAL, transaction.getAttachment().getTransactionType());
        Assert.assertFalse(updateTransaction.isUpdated());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveTwoUpdateTransactions() {
        repository.save(new UpdateTransaction(new IdTransaction(-4081443370478530685L), false));
        UpdateTransaction last = repository.getLast();
    }
    private class MockUpdaterMediator extends UpdaterMediatorImpl {
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
                public void endTransaction(Connection connection) {
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
            };
        }
    }
    class IdTransaction implements Transaction {
        private long id;

        public IdTransaction(long id) {
            this.id = id;
        }

        public IdTransaction(Transaction tr) {
            this.id = tr.getId();
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getStringId() {
            return null;
        }

        @Override
        public long getSenderId() {
            return 0;
        }

        @Override
        public byte[] getSenderPublicKey() {
            return new byte[0];
        }

        @Override
        public long getRecipientId() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public long getBlockId() {
            return 0;
        }

        @Override
        public Block getBlock() {
            return null;
        }

        @Override
        public short getIndex() {
            return 0;
        }

        @Override
        public int getTimestamp() {
            return 0;
        }

        @Override
        public int getBlockTimestamp() {
            return 0;
        }

        @Override
        public short getDeadline() {
            return 0;
        }

        @Override
        public int getExpiration() {
            return 0;
        }

        @Override
        public long getAmountATM() {
            return 0;
        }

        @Override
        public long getFeeATM() {
            return 0;
        }

        @Override
        public String getReferencedTransactionFullHash() {
            return null;
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getFullHash() {
            return null;
        }

        @Override
        public TransactionType getType() {
            return null;
        }

        @Override
        public Attachment getAttachment() {
            return null;
        }

        @Override
        public boolean verifySignature() {
            return false;
        }

        @Override
        public void validate() throws AplException.ValidationException {

        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

        @Override
        public byte[] getUnsignedBytes() {
            return new byte[0];
        }

        @Override
        public JSONObject getJSONObject() {
            return null;
        }

        @Override
        public JSONObject getPrunableAttachmentJSON() {
            return null;
        }

        @Override
        public byte getVersion() {
            return 0;
        }

        @Override
        public int getFullSize() {
            return 0;
        }

        @Override
        public Appendix.Message getMessage() {
            return null;
        }

        @Override
        public Appendix.EncryptedMessage getEncryptedMessage() {
            return null;
        }

        @Override
        public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {
            return null;
        }

        @Override
        public Appendix.Phasing getPhasing() {
            return null;
        }

        @Override
        public Appendix.PrunablePlainMessage getPrunablePlainMessage() {
            return null;
        }

        @Override
        public Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage() {
            return null;
        }

        @Override
        public List<? extends Appendix> getAppendages() {
            return null;
        }

        @Override
        public List<? extends Appendix> getAppendages(boolean includeExpiredPrunable) {
            return null;
        }

        @Override
        public List<? extends Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
            return null;
        }

        @Override
        public int getECBlockHeight() {
            return 0;
        }

        @Override
        public long getECBlockId() {
            return 0;
        }
    }
}
