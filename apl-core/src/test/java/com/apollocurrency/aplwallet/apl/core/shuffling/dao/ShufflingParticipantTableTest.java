/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import javax.inject.Inject;
@EnableWeld
public class ShufflingParticipantTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/shuffling.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            ShufflingParticipantTable.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();
    @Inject
    ShufflingParticipantTable table;
    ShufflingTestData std;


    @BeforeEach
    public void setUp() {
        std = new ShufflingTestData();
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
