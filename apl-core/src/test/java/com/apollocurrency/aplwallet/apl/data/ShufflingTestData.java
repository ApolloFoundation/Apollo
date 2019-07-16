/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.Stage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.ArrayList;
import java.util.List;

public class ShufflingTestData {
    public final byte[][] RECIPIENT_PUBLIC_KEYS = {Convert.parseHexString("51546eb53e8439f156acd2a7b7301cadec13d0ff85f46ff0cc97005ae16776b7"), Convert.parseHexString("4d04aabfa6588d866f8eaa3ebc30ae5012b49cd1bd7667c716068f16a79303ec"), Convert.parseHexString("977f3b11ad0373a63688dd416f991d2447bddaf3660403077282bb8bad9c01ab")};
    public final Shuffling SHUFFLING_1_1_ASSET_REGISTRATION    = new Shuffling(100L, 10_000L, 500   , HoldingType.get((byte) 1), 2500 , 100_000 , (byte) 10, (short) 120 , (byte) 2, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 999 );
    public final Shuffling SHUFFLING_2_1_APL_VERIF_DELETED     = new Shuffling(110L, 20_000L, 0     , HoldingType.get((byte) 0), 3500 , 2500    , (byte) 3 , (short) 1   , (byte) 3, Stage.VERIFICATION , 0   , RECIPIENT_PUBLIC_KEYS, 999 );
    public final Shuffling SHUFFLING_3_1_APL_REGISTRATION      = new Shuffling(120L, 30_000L, 0     , HoldingType.get((byte) 0), 1500 , 2500    , (byte) 5 , (short) 120 , (byte) 2, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 1000);
    public final Shuffling SHUFFLING_2_2_APL_DONE_DELETED      = new Shuffling(130L, 20_000L, 0     , HoldingType.get((byte) 0), 3500 , 2500    , (byte) 3 , (short) 0   , (byte) 3, Stage.DONE         , 0   , RECIPIENT_PUBLIC_KEYS, 1000);
    public final Shuffling SHUFFLING_4_1_APL_DONE              = new Shuffling(140L, 40_000L, 0L    , HoldingType.get((byte) 0), 2500L, 5000L   , (byte) 3 , (short) 118 , (byte) 3, Stage.VERIFICATION , 2500,  Convert.EMPTY_BYTES , 1000);
    public final Shuffling SHUFFLING_5_1_APL_PROCESSING        = new Shuffling(150L, 50_000L, 0L    , HoldingType.get((byte) 0), 2500L, 1000L   , (byte) 3 , (short) 120 , (byte) 3, Stage.PROCESSING   , 1500,  Convert.EMPTY_BYTES , 1000);
    public final Shuffling SHUFFLING_6_1_CURRENCY_REGISTRATION = new Shuffling(160L, 60_000L, 1000  , HoldingType.get((byte) 2), 3500 , 2000    , (byte) 3 , (short) 1440, (byte) 1, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 1001);
    public final Shuffling SHUFFLING_7_1_CURRENCY_DONE         = new Shuffling(170L, 70_000L, 1000L , HoldingType.get((byte) 2), 1500L, 2000L   , (byte) 3 , (short) 1   , (byte) 3, Stage.VERIFICATION , 0   , RECIPIENT_PUBLIC_KEYS, 1001);
    public final Shuffling SHUFFLING_8_1_CURRENCY_PROCESSING   = new Shuffling(180L, 80_000L, 1000L , HoldingType.get((byte) 2), 1500L, 3000L   , (byte) 4 , (short) 110 , (byte) 4, Stage.PROCESSING   , 2500, Convert.EMPTY_BYTES  , 1001);
    public final Shuffling SHUFFLING_4_2_APL_FINISHED          = new Shuffling(190L, 40_000L, 0L    , HoldingType.get((byte) 0), 2500L, 5000L   , (byte) 3 , (short) 0   , (byte) 3, Stage.DONE         , 0   , RECIPIENT_PUBLIC_KEYS, 1001);
    public final Shuffling SHUFFLING_3_2_APL_REGISTRATION      = new Shuffling(200L, 30_000L, 0     , HoldingType.get((byte) 0), 1500 , 2500    , (byte) 5 , (short) 119 , (byte) 2, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 1002);
    public final Shuffling SHUFFLING_7_2_CURRENCY_FINISHED     = new Shuffling(210L, 70_000L, 1000L , HoldingType.get((byte) 2), 1500L, 2000L   , (byte) 3 , (short) 0   , (byte) 3, Stage.DONE         , 0   , RECIPIENT_PUBLIC_KEYS, 1002);
    public final Shuffling SHUFFLING_1_2_ASSET_REGISTRATION    = new Shuffling(220L, 10_000L, 500   , HoldingType.get((byte) 1), 2500 , 100_000 , (byte) 10, (short) 118 , (byte) 5, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 1003);
    public final Shuffling SHUFFLING_3_3_APL_REGISTRATION      = new Shuffling(230L, 30_000L, 0     , HoldingType.get((byte) 0), 1500 , 2500    , (byte) 5 , (short) 118 , (byte) 3, Stage.REGISTRATION , 0   , Convert.EMPTY_BYTES  , 1005);

    public final Shuffling NEW_SHUFFLING = new Shuffling(231L, 100L, 0L, HoldingType.APL, 1500L, 2000L, (byte) 3, (short) 1440, (byte) 1, Stage.REGISTRATION, 0, Convert.EMPTY_BYTES, 100_000);
    public final List<Shuffling> all = new ArrayList<>(List.of(
            SHUFFLING_1_1_ASSET_REGISTRATION,
            SHUFFLING_2_1_APL_VERIF_DELETED,
            SHUFFLING_3_1_APL_REGISTRATION,
            SHUFFLING_2_2_APL_DONE_DELETED,
            SHUFFLING_4_1_APL_DONE,
            SHUFFLING_5_1_APL_PROCESSING,
            SHUFFLING_6_1_CURRENCY_REGISTRATION,
            SHUFFLING_7_1_CURRENCY_DONE,
            SHUFFLING_8_1_CURRENCY_PROCESSING,
            SHUFFLING_4_2_APL_FINISHED,
            SHUFFLING_3_2_APL_REGISTRATION,
            SHUFFLING_7_2_CURRENCY_FINISHED,
            SHUFFLING_1_2_ASSET_REGISTRATION,
            SHUFFLING_3_3_APL_REGISTRATION     ));
    {
        SHUFFLING_7_1_CURRENCY_DONE.setLatest(false);
        SHUFFLING_4_1_APL_DONE.setLatest(false);
        SHUFFLING_3_1_APL_REGISTRATION.setLatest(false);
        SHUFFLING_3_2_APL_REGISTRATION.setLatest(false);
        SHUFFLING_1_1_ASSET_REGISTRATION.setLatest(false);
        SHUFFLING_2_1_APL_VERIF_DELETED.setLatest(false);
        SHUFFLING_2_2_APL_DONE_DELETED.setLatest(false);
    }
}
