/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.ArrayList;
import java.util.List;


public class ShufflingTestData {
    public final long ALICE_ID = 1500;
    public final long BOB_ID = 2500;
    public final long CHUCK_ID = 3500;

    public final byte[][] RECIPIENT_PUBLIC_KEYS = {Convert.parseHexString("51546eb53e8439f156acd2a7b7301cadec13d0ff85f46ff0cc97005ae16776b7"), Convert.parseHexString("4d04aabfa6588d866f8eaa3ebc30ae5012b49cd1bd7667c716068f16a79303ec"), Convert.parseHexString("977f3b11ad0373a63688dd416f991d2447bddaf3660403077282bb8bad9c01ab")};
    public final byte[][] DATA = {
        Convert.parseHexString("30a93d63de4e418e858b8a8d2457001af3ae45e11eba8ab94b21651ca13cc0d8")
    };
    public final Shuffling SHUFFLING_1_1_APL_VERIF_DELETED = new Shuffling(100L, 20_000L, 0, HoldingType.get((byte) 0), CHUCK_ID, 2500, (byte) 3, (short) 1, (byte) 3, ShufflingStage.VERIFICATION, 0, RECIPIENT_PUBLIC_KEYS, 996);
    public final Shuffling SHUFFLING_2_1_ASSET_REGISTRATION = new Shuffling(110L, 10_000L, 500, HoldingType.get((byte) 1), BOB_ID, 100_000, (byte) 10, (short) 120, (byte) 2, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 999);
    public final Shuffling SHUFFLING_3_1_APL_REGISTRATION = new Shuffling(120L, 30_000L, 0, HoldingType.get((byte) 0), ALICE_ID, 2500, (byte) 5, (short) 120, (byte) 2, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 1000);
    public final Shuffling SHUFFLING_1_2_APL_DONE_DELETED = new Shuffling(130L, 20_000L, 0, HoldingType.get((byte) 0), CHUCK_ID, 2500, (byte) 3, (short) 0, (byte) 3, ShufflingStage.DONE, 0, RECIPIENT_PUBLIC_KEYS, 1000);
    public final Shuffling SHUFFLING_4_1_APL_DONE = new Shuffling(140L, 40_000L, 0L, HoldingType.get((byte) 0), BOB_ID, 5000L, (byte) 3, (short) 118, (byte) 3, ShufflingStage.VERIFICATION, BOB_ID, Convert.EMPTY_BYTES, 1000);
    public final Shuffling SHUFFLING_5_1_APL_PROCESSING = new Shuffling(150L, 50_000L, 0L, HoldingType.get((byte) 0), BOB_ID, 1000L, (byte) 3, (short) 120, (byte) 3, ShufflingStage.PROCESSING, ALICE_ID, Convert.EMPTY_BYTES, 1000);
    public final Shuffling SHUFFLING_6_1_CURRENCY_REGISTRATION = new Shuffling(160L, 60_000L, 1000, HoldingType.get((byte) 2), CHUCK_ID, 2000, (byte) 3, (short) 1440, (byte) 1, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 1001);
    public final Shuffling SHUFFLING_7_1_CURRENCY_DONE = new Shuffling(170L, 70_000L, 1000L, HoldingType.get((byte) 2), ALICE_ID, 2000L, (byte) 3, (short) 1, (byte) 3, ShufflingStage.VERIFICATION, 0, RECIPIENT_PUBLIC_KEYS, 1001);
    public final Shuffling SHUFFLING_8_1_CURRENCY_PROCESSING = new Shuffling(180L, 80_000L, 1000L, HoldingType.get((byte) 2), ALICE_ID, 3000L, (byte) 4, (short) 110, (byte) 4, ShufflingStage.PROCESSING, BOB_ID, Convert.EMPTY_BYTES, 1001);
    public final Shuffling SHUFFLING_4_2_APL_FINISHED = new Shuffling(190L, 40_000L, 0L, HoldingType.get((byte) 0), BOB_ID, 5000L, (byte) 3, (short) 0, (byte) 3, ShufflingStage.DONE, 0, RECIPIENT_PUBLIC_KEYS, 1001);
    public final Shuffling SHUFFLING_3_2_APL_REGISTRATION = new Shuffling(200L, 30_000L, 0, HoldingType.get((byte) 0), ALICE_ID, 2500, (byte) 5, (short) 119, (byte) 2, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 1002);
    public final Shuffling SHUFFLING_7_2_CURRENCY_FINISHED = new Shuffling(210L, 70_000L, 1000L, HoldingType.get((byte) 2), ALICE_ID, 2000L, (byte) 3, (short) 0, (byte) 3, ShufflingStage.DONE, 0, RECIPIENT_PUBLIC_KEYS, 1002);
    public final Shuffling SHUFFLING_2_2_ASSET_REGISTRATION = new Shuffling(220L, 10_000L, 500, HoldingType.get((byte) 1), BOB_ID, 100_000, (byte) 10, (short) 118, (byte) 5, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 1003);
    public final Shuffling SHUFFLING_3_3_APL_REGISTRATION = new Shuffling(230L, 30_000L, 0, HoldingType.get((byte) 0), ALICE_ID, 2500, (byte) 5, (short) 118, (byte) 3, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 1005);

    public final Shuffling NEW_SHUFFLING = new Shuffling(231L, 100L, 0L, HoldingType.APL, ALICE_ID, 2000L, (byte) 3, (short) 1440, (byte) 1, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 100_000);

    //                                     shuffling number __ participant name ( C - Chuck, A - Alice, B- Bob) ___ participant number
    public final ShufflingParticipant PARTICIPANT_0_C_1_DELETED = new ShufflingParticipant(920L, 850, 12345, CHUCK_ID, 0, ALICE_ID, ShufflingParticipantState.get((byte) 2), Convert.parseHexString("5443a31df402f76ef4ff80fe7cbda419737b4d8933958fd5ce2c7e19b7daa58b"), Convert.parseHexString("b08dcd79a56d5ae3c92b6b25e60237d45a9c5e699174da17abf8b39bb2737afd"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_0_C_2_DELETED = new ShufflingParticipant(930L, 900, 12345, CHUCK_ID, 0, ALICE_ID, ShufflingParticipantState.get((byte) 2), Convert.parseHexString("5443a31df402f76ef4ff80fe7cbda419737b4d8933958fd5ce2c7e19b7daa58b"), Convert.parseHexString("b08dcd79a56d5ae3c92b6b25e60237d45a9c5e699174da17abf8b39bb2737afd"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_C_1_REGISTR = new ShufflingParticipant(940L, 994, SHUFFLING_1_2_APL_DONE_DELETED.getId(), CHUCK_ID, 0, BOB_ID, ShufflingParticipantState.get((byte) 0), Convert.parseHexString("6226f1365b784db87fa749ca6f793ebb9d1d7bed51ffd050d2c8767b4c4f9ec6"), Convert.parseHexString("00a3021a6e02ffe456d930bc789640b7b12b833d1bb621c781112f6007070d15"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_B_1_REGISTR = new ShufflingParticipant(950L, 995, SHUFFLING_1_2_APL_DONE_DELETED.getId(), BOB_ID, 1, ALICE_ID, ShufflingParticipantState.get((byte) 0), Convert.parseHexString("41a7ed1ba26217cf70059964c74665d0a9c364a4078f69a3ca6d1e2623b0679f"), Convert.parseHexString("3f048d92be9bf806374a1823d99cfdbe1c59fbcbdf1de40001f4e39df523a4e7"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_A_1_REGISTR = new ShufflingParticipant(960L, 996, SHUFFLING_1_2_APL_DONE_DELETED.getId(), ALICE_ID, 2, 0, ShufflingParticipantState.get((byte) 0), Convert.parseHexString("ecf13d204f0d98964d34959f2e5565cf9a909a5d1592004054a470e12147b6ab"), Convert.parseHexString("9ad9df94c1225e30c0d7cddd79a676c6a42371c52780031610941783eac8b9d1"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_C_2_PROCESS = new ShufflingParticipant(970L, 997, SHUFFLING_1_2_APL_DONE_DELETED.getId(), CHUCK_ID, 0, BOB_ID, ShufflingParticipantState.get((byte) 1), Convert.parseHexString("6226f1365b784db87fa749ca6f793ebb9d1d7bed51ffd050d2c8767b4c4f9ec6"), Convert.parseHexString("00a3021a6e02ffe456d930bc789640b7b12b833d1bb621c781112f6007070d15"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_B_2_PROCESS = new ShufflingParticipant(980L, 998, SHUFFLING_1_2_APL_DONE_DELETED.getId(), BOB_ID, 1, ALICE_ID, ShufflingParticipantState.get((byte) 1), Convert.parseHexString("41a7ed1ba26217cf70059964c74665d0a9c364a4078f69a3ca6d1e2623b0679f"), Convert.parseHexString("3f048d92be9bf806374a1823d99cfdbe1c59fbcbdf1de40001f4e39df523a4e7"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_A_2_PROCESS = new ShufflingParticipant(990L, 999, SHUFFLING_1_2_APL_DONE_DELETED.getId(), ALICE_ID, 2, 0, ShufflingParticipantState.get((byte) 1), Convert.parseHexString("ecf13d204f0d98964d34959f2e5565cf9a909a5d1592004054a470e12147b6ab"), Convert.parseHexString("9ad9df94c1225e30c0d7cddd79a676c6a42371c52780031610941783eac8b9d1"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_2_B_1_REGISTR = new ShufflingParticipant(1000L, 999, SHUFFLING_2_1_ASSET_REGISTRATION.getId(), BOB_ID, 0, 0, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_3_A_1_REGISTR = new ShufflingParticipant(1010L, 1000, SHUFFLING_3_1_APL_REGISTRATION.getId(), ALICE_ID, 0, 0, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_7_A_1_REGISTR = new ShufflingParticipant(1020L, 1000, SHUFFLING_7_1_CURRENCY_DONE.getId(), ALICE_ID, 0, 0, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_8_A_1_REGISTR = new ShufflingParticipant(1030L, 1000, SHUFFLING_8_1_CURRENCY_PROCESSING.getId(), ALICE_ID, 0, 0, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_C_3_VERIFIC = new ShufflingParticipant(1040L, 1000, SHUFFLING_1_2_APL_DONE_DELETED.getId(), CHUCK_ID, 0, BOB_ID, ShufflingParticipantState.get((byte) 2), Convert.parseHexString("6226f1365b784db87fa749ca6f793ebb9d1d7bed51ffd050d2c8767b4c4f9ec6"), Convert.parseHexString("00a3021a6e02ffe456d930bc789640b7b12b833d1bb621c781112f6007070d15"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_1_B_3_VERIFIC = new ShufflingParticipant(1050L, 1000, SHUFFLING_1_2_APL_DONE_DELETED.getId(), BOB_ID, 1, ALICE_ID, ShufflingParticipantState.get((byte) 2), Convert.parseHexString("41a7ed1ba26217cf70059964c74665d0a9c364a4078f69a3ca6d1e2623b0679f"), Convert.parseHexString("3f048d92be9bf806374a1823d99cfdbe1c59fbcbdf1de40001f4e39df523a4e7"), Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_2_B_2_REGISTR = new ShufflingParticipant(1060L, 1001, SHUFFLING_2_1_ASSET_REGISTRATION.getId(), BOB_ID, 0, CHUCK_ID, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant PARTICIPANT_2_C_1_REGISTR = new ShufflingParticipant(1070L, 1001, SHUFFLING_2_1_ASSET_REGISTRATION.getId(), CHUCK_ID, 1, 0, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);
    public final ShufflingParticipant NEW_PARTICIPANT = new ShufflingParticipant(1071L, 1000, SHUFFLING_3_3_APL_REGISTRATION.getId(), BOB_ID, 1, ALICE_ID, ShufflingParticipantState.get((byte) 0), null, null, Convert.EMPTY_BYTES, Convert.EMPTY_BYTES);

    public final ShufflingData DATA_5_A = new ShufflingData(1000L, 1001, 10, SHUFFLING_5_1_APL_PROCESSING.getId(), ALICE_ID, DATA, 800);

    public final ShufflingData NEW_DATA = new ShufflingData(1001L, 1002, 20, SHUFFLING_5_1_APL_PROCESSING.getId(), BOB_ID, DATA, 821);


    public final List<ShufflingParticipant> ALL_PARTICIPANTS = new ArrayList<>(List.of(
        PARTICIPANT_0_C_1_DELETED,
        PARTICIPANT_0_C_2_DELETED,
        PARTICIPANT_1_C_1_REGISTR,
        PARTICIPANT_1_B_1_REGISTR,
        PARTICIPANT_1_A_1_REGISTR,
        PARTICIPANT_1_C_2_PROCESS,
        PARTICIPANT_1_B_2_PROCESS,
        PARTICIPANT_1_A_2_PROCESS,
        PARTICIPANT_2_B_1_REGISTR,
        PARTICIPANT_3_A_1_REGISTR,
        PARTICIPANT_7_A_1_REGISTR,
        PARTICIPANT_8_A_1_REGISTR,
        PARTICIPANT_2_B_2_REGISTR,
        PARTICIPANT_2_C_1_REGISTR,
        PARTICIPANT_1_C_3_VERIFIC,
        PARTICIPANT_1_B_3_VERIFIC
    ));

    {
        PARTICIPANT_0_C_1_DELETED.setLatest(false);
        PARTICIPANT_0_C_2_DELETED.setLatest(false);
        PARTICIPANT_1_C_1_REGISTR.setLatest(false);
        PARTICIPANT_1_B_1_REGISTR.setLatest(false);
        PARTICIPANT_1_A_1_REGISTR.setLatest(false);
        PARTICIPANT_1_C_2_PROCESS.setLatest(false);
        PARTICIPANT_1_B_2_PROCESS.setLatest(false);
        PARTICIPANT_2_B_1_REGISTR.setLatest(false);
    }

    public final List<Shuffling> ALL_SHUFFLINGS = new ArrayList<>(List.of(
        SHUFFLING_2_1_ASSET_REGISTRATION,
        SHUFFLING_1_1_APL_VERIF_DELETED,
        SHUFFLING_3_1_APL_REGISTRATION,
        SHUFFLING_1_2_APL_DONE_DELETED,
        SHUFFLING_4_1_APL_DONE,
        SHUFFLING_5_1_APL_PROCESSING,
        SHUFFLING_6_1_CURRENCY_REGISTRATION,
        SHUFFLING_7_1_CURRENCY_DONE,
        SHUFFLING_8_1_CURRENCY_PROCESSING,
        SHUFFLING_4_2_APL_FINISHED,
        SHUFFLING_3_2_APL_REGISTRATION,
        SHUFFLING_7_2_CURRENCY_FINISHED,
        SHUFFLING_2_2_ASSET_REGISTRATION,
        SHUFFLING_3_3_APL_REGISTRATION));

    {
        SHUFFLING_7_1_CURRENCY_DONE.setLatest(false);
        SHUFFLING_4_1_APL_DONE.setLatest(false);
        SHUFFLING_3_1_APL_REGISTRATION.setLatest(false);
        SHUFFLING_3_2_APL_REGISTRATION.setLatest(false);
        SHUFFLING_2_1_ASSET_REGISTRATION.setLatest(false);
        SHUFFLING_1_1_APL_VERIF_DELETED.setLatest(false);
        SHUFFLING_1_1_APL_VERIF_DELETED.setDeleted(true);
        SHUFFLING_1_2_APL_DONE_DELETED.setLatest(false);
        SHUFFLING_1_2_APL_DONE_DELETED.setDeleted(true);
        ALL_SHUFFLINGS.forEach(e->e.setDbKey(new LongKey(e.getId())));
    }
}

