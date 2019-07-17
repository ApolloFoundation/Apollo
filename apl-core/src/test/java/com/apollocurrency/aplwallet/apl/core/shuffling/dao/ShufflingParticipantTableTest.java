/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import javax.inject.Inject;
@EnableWeld
public class ShufflingParticipantTableTest extends VersionedEntityDbTableTest<ShufflingParticipant> {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            FullTextConfigImpl.class,
            ShufflingParticipantTable.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    Blockchain blockchain;
    @Inject
    ShufflingParticipantTable table;
    ShufflingTestData std;

    public ShufflingParticipantTableTest() {
        super(ShufflingParticipant.class);
    }

    @Override
    @BeforeEach
    public void setUp() {
        std = new ShufflingTestData();
        super.setUp();
    }

    @Override
    public DerivedDbTable<ShufflingParticipant> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<ShufflingParticipant> getAll() {
        return std.ALL_PARTICIPANTS;
    }

    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public ShufflingParticipant valueToInsert() {
        return std.NEW_PARTICIPANT;
    }
    @Override
    protected String dataScriptPath() {
        return "db/shuffling.sql";
    }

    @Test
    void testGetParticipants() {
        List<ShufflingParticipant> participants = table.getParticipants(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId());

        assertEquals(List.of(std.PARTICIPANT_1_1_C_3_VERIFIC, std.PARTICIPANT_1_1_B_3_VERIFIC, std.PARTICIPANT_1_0_A_2_PROCESS), participants);
    }

    @Test
    void testGetParticipantsForNonExistentShuffling() {
        List<ShufflingParticipant> shufflings = table.getParticipants(Long.MAX_VALUE);
        assertEquals(0, shufflings.size());
    }

    @Test
    void testGetByIndex() {
        ShufflingParticipant participant = table.getByIndex(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId(), 2);

        assertEquals(std.PARTICIPANT_1_0_A_2_PROCESS, participant);
    }

    @Test
    void testGetByIndexWhichNotExist() {
        ShufflingParticipant participant = table.getByIndex(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId(), 3);

        assertNull(participant);
    }

    @Test
    void testGetLastParticipant() {
        ShufflingParticipant last = table.getLast(std.SHUFFLING_2_2_ASSET_REGISTRATION.getId());

        assertEquals(std.PARTICIPANT_2_1_C_1_REGISTR, last);
    }

    @Test
    void testGetLastParticipantWhichNotExists() {
        ShufflingParticipant last = table.getLast(Long.MAX_VALUE);

        assertNull(last);
    }

    @Test
    void testGetVerifiedCount() {
        int verifiedCount = table.getVerifiedCount(std.SHUFFLING_1_2_APL_DONE_DELETED.getId());

        assertEquals(2, verifiedCount);
    }

    @Test
    void testGetVerifiedCountForShufflingDuringRegistration() {
        int verifiedCount = table.getVerifiedCount(std.SHUFFLING_2_2_ASSET_REGISTRATION.getId());

        assertEquals(0, verifiedCount);
    }

    @Test
    void testGetByShufflingIdAndAccountId() {
        ShufflingParticipant participant = table.get(std.PARTICIPANT_2_1_B_1_REGISTR.getShufflingId(), std.PARTICIPANT_2_1_B_1_REGISTR.getAccountId());

        assertEquals(std.PARTICIPANT_2_1_B_2_REGISTR, participant);
    }

}
