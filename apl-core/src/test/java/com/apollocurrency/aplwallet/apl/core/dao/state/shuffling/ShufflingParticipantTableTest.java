/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerRootUserTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@Tag("slow")
public class ShufflingParticipantTableTest extends DbContainerRootUserTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/shuffling.sql", null);


    ShufflingParticipantTable table = new ShufflingParticipantTable(extension.getDatabaseManager(), mock(Event.class));
    ShufflingTestData std;


    @BeforeEach
    public void setUp() {
        std = new ShufflingTestData();
    }

    @Test
    void testGetParticipants() {
        List<ShufflingParticipant> participants = table.getParticipants(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId());

        assertEquals(List.of(std.PARTICIPANT_1_C_3_VERIFIC, std.PARTICIPANT_1_B_3_VERIFIC, std.PARTICIPANT_1_A_2_PROCESS), participants);
    }

    @Test
    void testGetParticipantsForNonExistentShuffling() {
        List<ShufflingParticipant> shufflings = table.getParticipants(Long.MAX_VALUE);
        assertEquals(0, shufflings.size());
    }

    @Test
    void testGetByIndex() {
        ShufflingParticipant participant = table.getByIndex(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId(), 2);

        assertEquals(std.PARTICIPANT_1_A_2_PROCESS, participant);
    }

    @Test
    void testGetByIndexWhichNotExist() {
        ShufflingParticipant participant = table.getByIndex(std.SHUFFLING_1_1_APL_VERIF_DELETED.getId(), 3);

        assertNull(participant);
    }

    @Test
    void testGetLastParticipant() {
        ShufflingParticipant last = table.getLast(std.SHUFFLING_2_2_ASSET_REGISTRATION.getId());

        assertEquals(std.PARTICIPANT_2_C_1_REGISTR, last);
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
        ShufflingParticipant participant = table.getParticipant(std.PARTICIPANT_2_B_1_REGISTR.getShufflingId(), std.PARTICIPANT_2_B_1_REGISTR.getAccountId());

        assertEquals(std.PARTICIPANT_2_B_2_REGISTR, participant);
    }

    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_PARTICIPANT));

        ShufflingParticipant savedParticipant = table.getParticipant(std.NEW_PARTICIPANT.getShufflingId(), std.NEW_PARTICIPANT.getAccountId());
        assertEquals(std.NEW_PARTICIPANT, savedParticipant);

        std.NEW_PARTICIPANT.setDbKey(new LinkKey(std.NEW_PARTICIPANT.getShufflingId(), std.NEW_PARTICIPANT.getAccountId()));
        std.NEW_PARTICIPANT.setHeight(std.NEW_PARTICIPANT.getHeight() + 1);

        DbUtils.inTransaction(extension, (con) -> table.insert(std.NEW_PARTICIPANT));
        std.NEW_PARTICIPANT.setDbId(std.NEW_PARTICIPANT.getDbId() + 1);

        savedParticipant = table.getParticipant(std.NEW_PARTICIPANT.getShufflingId(), std.NEW_PARTICIPANT.getAccountId());
        assertEquals(std.NEW_PARTICIPANT, savedParticipant);

    }

}
