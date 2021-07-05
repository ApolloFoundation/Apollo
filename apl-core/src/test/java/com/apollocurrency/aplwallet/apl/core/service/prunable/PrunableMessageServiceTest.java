/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.prunable;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.TwoTablesPublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PrunableMessageTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class PrunableMessageServiceTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbConfig(), null, "db/prunable-message-data.sql");
    Blockchain blockchain = mock(Blockchain.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
        PrunableMessageTable.class,
        DerivedDbTablesRegistryImpl.class,
        PrunableMessageServiceImpl.class,
        FullTextConfigImpl.class,
        PropertiesHolder.class,
        AccountPublicKeyServiceImpl.class,
        PublicKeyTableProducer.class,
        TwoTablesPublicKeyDao.class,
        BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class
    )
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .build();
    @Inject
    PrunableMessageService service;
    @Inject
    AccountPublicKeyService accountPublicKeyService;

    PrunableMessageTestData data = new PrunableMessageTestData();

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Test
    void testGetCount() {
        int expectedSize = data.ALL.size();

        int count = service.getCount();

        assertEquals(expectedSize, count);
    }

    @Test
    void testGetAll() {
        List<PrunableMessage> all = service.getAll(2, 6);
        assertEquals(List.of(data.MESSAGE_9, data.MESSAGE_8, data.MESSAGE_7, data.MESSAGE_6, data.MESSAGE_5), all);
    }

    @Test
    void testGetByTxId() {
        PrunableMessage prunableMessage = service.get(data.MESSAGE_11.getId());
        assertEquals(data.MESSAGE_11, prunableMessage);
    }

    @Test
    void testGetAllForAccount() {
        List<PrunableMessage> all = service.getAll(data.BOB_ID, 1, 4);
        assertEquals(List.of(data.MESSAGE_9, data.MESSAGE_7, data.MESSAGE_4, data.MESSAGE_3), all);
    }

    @Test
    void testGetAllForSenderAndRecipient() {
        List<PrunableMessage> all = service.getAll(data.ALICE_ID, data.BOB_ID, 2, 3);
        assertEquals(List.of(data.MESSAGE_2, data.MESSAGE_1), all);
    }

    @Test
    void testGetAllEncryptToSelf() {
        List<PrunableMessage> all = service.getAll(data.ALICE_ID, data.ALICE_ID, 0, Integer.MAX_VALUE);
        assertEquals(List.of(data.MESSAGE_5), all);
    }

    @Test
    void testDecrypt() {
        byte[] decryptedBytes = service.decrypt(data.MESSAGE_2, data.BOB_PASSPHRASE);
        assertEquals(data.DECRYPTED_MESSAGE_2, new String(decryptedBytes));
    }

    @Test
    void testDecryptCompressedUsingSharedKey() {
        byte[] decryptedBytes = service.decryptUsingSharedKey(data.MESSAGE_1, data.MESSAGE_1_SHARED_KEY);
        assertEquals(data.DECRYPTED_MESSAGE_1, new String(decryptedBytes));
    }

    @Test
    void testDecryptOrdinaryUsingSharedKey() {
        byte[] decryptedBytes = service.decryptUsingSharedKey(data.MESSAGE_4, data.MESSAGE_4_SHARED_KEY);
        assertEquals(data.DECRYPTED_MESSAGE_4, new String(decryptedBytes));
    }

    @Test
    void testDecryptNotEncryptedMessageUsingSharedKey() {
        byte[] decryptedBytes = service.decryptUsingSharedKey(data.MESSAGE_6, data.MESSAGE_4_SHARED_KEY);
        assertNull(decryptedBytes);
    }

    @Test
    void testDecryptUsingKeySeedByRecipient() {
        byte[] decryptedBytes = service.decryptUsingKeySeed(data.MESSAGE_9, Crypto.getKeySeed(data.BOB_PASSPHRASE));
        assertEquals(data.DECRYPTED_MESSAGE_9, new String(decryptedBytes));
    }

    @Test
    void testDecryptUsingKeySeedBySender() {
        byte[] decryptedBytes = service.decryptUsingKeySeed(data.MESSAGE_9, Crypto.getKeySeed(data.CHUCK_PASSPHRASE));
        assertEquals(data.DECRYPTED_MESSAGE_9, new String(decryptedBytes));
    }

    @Test
    void testDecryptNotEncryptedMessageUsingKeySeed() {
        byte[] decryptedBytes = service.decryptUsingKeySeed(data.MESSAGE_11, Crypto.getKeySeed(data.ALICE_PASSPHRASE));
        assertNull(decryptedBytes);
    }

    @Test
    void testAddPlain() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.ALICE_ID).when(tx).getRecipientId();
        doReturn(data.CHUCK_ID).when(tx).getSenderId();
        doReturn(120L).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp() + 10).when(tx).getTimestamp();
        doReturn(data.MESSAGE_11.getHeight() + 1).when(blockchain).getHeight();
        doReturn(data.MESSAGE_11.getBlockTimestamp() + 15).when(blockchain).getLastBlockTimestamp();
        PrunablePlainMessageAppendix message = new PrunablePlainMessageAppendix("New public prunable message");
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message));
        PrunableMessage prunableMessage = service.get(120L);
        assertNotNull(prunableMessage);
        assertEquals(new PrunableMessage(data.MESSAGE_11.getDbId() + 1, 120L, data.CHUCK_ID, data.ALICE_ID, message.getMessage(), null, true, false, false, data.MESSAGE_11.getBlockTimestamp() + 15, data.MESSAGE_11.getTransactionTimestamp() + 10, data.MESSAGE_11.getHeight() + 1), prunableMessage);
    }

    @Test
    void testAddNullPlain() {
        DbUtils.inTransaction(extension, (con) -> service.add(mock(Transaction.class), new PrunablePlainMessageAppendix((byte[]) null), 10, 10));
        assertEquals(data.ALL.size(), service.getCount());
    }

    @Test
    void testModifyExistingUsingDifferentHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.ALICE_ID).when(tx).getRecipientId();
        doReturn(data.CHUCK_ID).when(tx).getSenderId();
        doReturn(data.MESSAGE_11.getId()).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp() + 10).when(tx).getTimestamp();
        PrunablePlainMessageAppendix message = new PrunablePlainMessageAppendix("New public prunable message");
        assertThrows(RuntimeException.class, () -> service.add(tx, message, data.MESSAGE_11.getBlockTimestamp() + 15, data.MESSAGE_11.getHeight() + 1));
    }

    @Test
    void testModifyExistingUsingSameHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.MESSAGE_10.getId()).when(tx).getId();
        doReturn(data.MESSAGE_10.getTransactionTimestamp()).when(tx).getTimestamp();
        String publicMessage = "New public prunable message";
        PrunablePlainMessageAppendix message = new PrunablePlainMessageAppendix(publicMessage);
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message, data.MESSAGE_10.getBlockTimestamp(), data.MESSAGE_10.getHeight()));
        PrunableMessage prunableMessage = service.get(data.MESSAGE_10.getId());
        assertNotNull(prunableMessage);
        data.MESSAGE_10.setMessage(publicMessage.getBytes());
        data.MESSAGE_10.setMessageIsText(true);
        assertEquals(data.MESSAGE_10, prunableMessage);
    }

    @Test
    void testModifyExistingWithPlainMessageUsingSameHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.MESSAGE_11.getId()).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp()).when(tx).getTimestamp();
        String publicMessage = "New public prunable message";
        PrunablePlainMessageAppendix message = new PrunablePlainMessageAppendix(publicMessage);
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message, data.MESSAGE_11.getBlockTimestamp(), data.MESSAGE_11.getHeight()));
        PrunableMessage prunableMessage = service.get(data.MESSAGE_11.getId());
        assertNotNull(prunableMessage);
        assertEquals(data.MESSAGE_11, prunableMessage);
    }

    @Test
    void testAddEncrypted() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.ALICE_ID).when(tx).getRecipientId();
        doReturn(data.CHUCK_ID).when(tx).getSenderId();
        doReturn(120L).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp() + 10).when(tx).getTimestamp();
        doReturn(data.MESSAGE_11.getHeight() + 1).when(blockchain).getHeight();
        doReturn(data.MESSAGE_11.getBlockTimestamp() + 15).when(blockchain).getLastBlockTimestamp();
        PrunableEncryptedMessageAppendix message = new PrunableEncryptedMessageAppendix(data.MESSAGE_2.getEncryptedData(), data.MESSAGE_2.encryptedMessageIsText(), data.MESSAGE_2.isCompressed());
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message));
        PrunableMessage prunableMessage = service.get(120L);
        assertNotNull(prunableMessage);
        assertEquals(new PrunableMessage(data.MESSAGE_11.getDbId() + 1, 120L, data.CHUCK_ID, data.ALICE_ID, null, message.getEncryptedData(), false, message.isText(), message.isCompressed(), data.MESSAGE_11.getBlockTimestamp() + 15, data.MESSAGE_11.getTransactionTimestamp() + 10, data.MESSAGE_11.getHeight() + 1), prunableMessage);
    }

    @Test
    void testAddNullEncrypted() {
        service.add(mock(Transaction.class), new PrunableEncryptedMessageAppendix(null, false, false), 100, 125);
        int count = service.getCount();
        assertEquals(data.ALL.size(), count);
    }

    @Test
    void testTryToAddEncryptedToExistingUsingDifferentHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.ALICE_ID).when(tx).getRecipientId();
        doReturn(data.CHUCK_ID).when(tx).getSenderId();
        doReturn(data.MESSAGE_11.getId()).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp() + 10).when(tx).getTimestamp();
        PrunableEncryptedMessageAppendix message = new PrunableEncryptedMessageAppendix(data.MESSAGE_2.getEncryptedData(), data.MESSAGE_2.encryptedMessageIsText(), data.MESSAGE_2.isCompressed());
        assertThrows(RuntimeException.class, () -> service.add(tx, message, data.MESSAGE_11.getBlockTimestamp() + 15, data.MESSAGE_11.getHeight() + 1));
    }

    @Test
    void testTryToAddEncryptedToExistingUsingSameHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.MESSAGE_11.getId()).when(tx).getId();
        doReturn(data.MESSAGE_11.getTransactionTimestamp()).when(tx).getTimestamp();
        PrunableEncryptedMessageAppendix message = new PrunableEncryptedMessageAppendix(data.MESSAGE_2.getEncryptedData(), data.MESSAGE_2.encryptedMessageIsText(), data.MESSAGE_2.isCompressed());
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message, data.MESSAGE_11.getBlockTimestamp(), data.MESSAGE_11.getHeight()));
        PrunableMessage prunableMessage = service.get(data.MESSAGE_11.getId());
        assertNotNull(prunableMessage);
        data.MESSAGE_11.setEncryptedData(message.getEncryptedData());
        data.MESSAGE_11.setEncryptedMessageIsText(message.isText());
        data.MESSAGE_11.setCompressed(message.isCompressed());
        assertEquals(data.MESSAGE_11, prunableMessage);
    }

    @Test
    void testTryToAddEncryptedToExistingWithEncryptedUsingSameHeight() {
        Transaction tx = mock(Transaction.class);
        doReturn(data.MESSAGE_10.getId()).when(tx).getId();
        doReturn(data.MESSAGE_10.getTransactionTimestamp()).when(tx).getTimestamp();
        PrunableEncryptedMessageAppendix message = new PrunableEncryptedMessageAppendix(data.MESSAGE_4.getEncryptedData(), data.MESSAGE_4.encryptedMessageIsText(), data.MESSAGE_4.isCompressed());
        DbUtils.inTransaction(extension, (con) -> service.add(tx, message, data.MESSAGE_10.getBlockTimestamp(), data.MESSAGE_10.getHeight()));
        PrunableMessage prunableMessage = service.get(data.MESSAGE_10.getId());
        assertNotNull(prunableMessage);
        assertEquals(data.MESSAGE_10, prunableMessage);
    }

    @Test
    void testIsPruned() {
        boolean pruned = service.isPruned(data.MESSAGE_5.getId(), false, true);
        assertFalse(pruned);
    }

}
