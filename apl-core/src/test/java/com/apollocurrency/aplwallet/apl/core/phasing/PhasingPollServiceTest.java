/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollServiceTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            PhasingPollServiceImpl.class,
            GlobalSyncImpl.class,
            PhasingPollResultTable.class,
            PhasingPollTable.class ,
            PhasingPollVoterTable.class ,
            PhasingPollLinkedTransactionTable.class ,
            PhasingVoteTable.class ,
            FullTextConfig.class,
            DerivedDbTablesRegistry.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();
    @Inject
    PhasingPollService phasingPollService;
    PhasingTestData     ptd;
    TransactionTestData ttd;


    @Inject
    JdbiHandleFactory jdbiHandleFactory;
    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    void setUp() {
        System.out.println(Thread.currentThread().getId());
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
    }

    @Test
    void testGetActivePhasingDbIds() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_8.getHeight() + 1);
        assertEquals(Arrays.asList(ttd.DB_ID_8, ttd.DB_ID_7), dbIds);
    }
    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_12.getHeight() + 1);
        assertEquals(Arrays.asList(ttd.DB_ID_12), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdAllPollsFinished() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ptd.POLL_2.getFinishHeight() + 1);
        assertEquals(Collections.emptyList(), dbIds);
    }
    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHeight() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_0.getHeight());
        assertEquals(Collections.emptyList(), dbIds);
    }

    @Test
    void testGetPoll() {
        PhasingPoll poll = phasingPollService.getPoll(ptd.POLL_0.getId());

        assertNotNull(poll);
        assertEquals(ptd.POLL_0, poll);
    }
}
