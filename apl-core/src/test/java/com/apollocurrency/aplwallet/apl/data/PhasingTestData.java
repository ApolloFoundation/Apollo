/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollResult;

public class PhasingTestData {
    public final PhasingPoll POLL_0 = new PhasingPoll(3444674909301056677L, 9211698109297098287L, (byte) 1, 2000, (byte) 0, 1, null, null, (byte) 0, null, (byte) 0);
    public final PhasingPoll POLL_1 = new PhasingPoll(3746857886535243786L, 9211698109297098287L, (byte) 1, 5000, (byte) 0, 1, null, null, (byte) 0, null, (byte) 0);
    public final PhasingPoll POLL_2 = new PhasingPoll(2083198303623116770L, 9211698109297098287L, (byte) 1, 9500, (byte) 0, 1, null, null, (byte) 0, null, (byte) 0);
    public final PhasingPoll POLL_3 = new PhasingPoll(-4081443370478530685L, 9211698109297098287L, (byte) 1, 17000, (byte) 0, 1, null, null, (byte) 0, null, (byte) 0);
    public final PhasingPollResult RESULT_0 = new PhasingPollResult(3444674909301056677L, 1, true, 1500);
    public final PhasingPollResult RESULT_1 = new PhasingPollResult(3746857886535243786L, 0, true, 4000);
    public final PhasingPollResult RESULT_2 = new PhasingPollResult(2083198303623116770L, 0, false, 9500);
    public final long DB_ID_0  = 150;
    public final long DB_ID_1  = 175;
    public final long DB_ID_2  = 200;
    public final long DB_ID_3  = 500;
    public final long DB_ID_4  = 1000;
    public final long DB_ID_5  = 1500;
    public final long DB_ID_6  = 2000;
    public final long DB_ID_7  = 2500;
    public final long DB_ID_8  = 3000;
    public final long DB_ID_9  = 3500;
    public final long DB_ID_10 = 4000;
    public final long DB_ID_11 = 4500;
    public final long DB_ID_12 = 5000;
}

