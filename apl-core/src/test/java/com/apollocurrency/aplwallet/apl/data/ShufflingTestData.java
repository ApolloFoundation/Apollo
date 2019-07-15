/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.ShufflingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

public class ShufflingTestData {
    public final Shuffling APL_SHUFFLING1_1 = new Shuffling(100L, 10_000L, 0, HoldingType.get((byte) 0), 1500, 2500, (byte) 5, (short) 120, (byte) 2, ShufflingService.Stage.REGISTRATION, 0, null, 1000);
    public final Shuffling APL_SHUFFLING1_2 = new Shuffling(110L, 10_000L, 0, HoldingType.get((byte) 0), 1500, 2500, (byte) 5, (short) 119, (byte) 2, ShufflingService.Stage.REGISTRATION, 0, null, 1002);
    public final Shuffling APL_SHUFFLING1_3 = new Shuffling(120L, 10_000L, 0, HoldingType.get((byte) 0), 1500, 2500, (byte) 5, (short) 118, (byte) 3, ShufflingService.Stage.REGISTRATION, 0, null, 1005);
    public final Shuffling ASSET_SHUFFLING2_1 = new Shuffling(130L, 20_000L, 500, HoldingType.get((byte) 1), 2500, 100_000, (byte) 10, (short) 120, (byte) 2, ShufflingService.Stage.REGISTRATION, 0, null, 999);
    public final Shuffling ASSET_SHUFFLING2_2 = new Shuffling(140L, 20_000L, 500, HoldingType.get((byte) 1), 2500, 100_000, (byte) 10, (short) 118, (byte) 5, ShufflingService.Stage.REGISTRATION, 0, null, 1003);
    public final Shuffling CURRENCY_SHUFFLING3_1 = new Shuffling(150L, 30_000L, 1000, HoldingType.get((byte) 2), 3500, 2000, (byte) 3, (short) 1440, (byte) 1, ShufflingService.Stage.REGISTRATION, 0, null, 1001);
    public final Shuffling DELETED_SHUFFLING4_1 = new Shuffling(160L, 40_000L, 0, HoldingType.get((byte) 0), 3500, 2500, (byte) 3, (short) 1, (byte) 3, ShufflingService.Stage.DONE, 0, new byte[][]{Convert.parseHexString("51546eb53e8439f156acd2a7b7301cadec13d0ff85f46ff0cc97005ae16776b7"), Convert.parseHexString("4d04aabfa6588d866f8eaa3ebc30ae5012b49cd1bd7667c716068f16a79303ec"), Convert.parseHexString("977f3b11ad0373a63688dd416f991d2447bddaf3660403077282bb8bad9c01ab")}, 999);
    public final Shuffling DELETED_SHUFFLING4_2 = new Shuffling(170L, 40_000L, 0, HoldingType.get((byte) 0), 3500, 2500, (byte) 3, (short) 0, (byte) 3, ShufflingService.Stage.DONE, 0, new byte[][]{Convert.parseHexString("51546eb53e8439f156acd2a7b7301cadec13d0ff85f46ff0cc97005ae16776b7"), Convert.parseHexString("4d04aabfa6588d866f8eaa3ebc30ae5012b49cd1bd7667c716068f16a79303ec"), Convert.parseHexString("977f3b11ad0373a63688dd416f991d2447bddaf3660403077282bb8bad9c01ab")}, 1000);
    {
        APL_SHUFFLING1_1.setLatest(false);
        APL_SHUFFLING1_2.setLatest(false);
        ASSET_SHUFFLING2_1.setLatest(false);
        DELETED_SHUFFLING4_1.setLatest(false);
        DELETED_SHUFFLING4_2.setLatest(false);
    }
}
