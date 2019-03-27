/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollResult;

public class PhasingTestData {
    public final PhasingPoll POLL_0;
    public final PhasingPoll POLL_1;
    public final PhasingPoll POLL_2;
    public final PhasingPoll POLL_3;
    public final PhasingPollResult RESULT_0;
    public final PhasingPollResult RESULT_1;
    public final PhasingPollResult RESULT_2;
    private final TransactionTestData td;


    public PhasingTestData() {
        td = new TransactionTestData();
        POLL_0 = new PhasingPoll(td.TRANSACTION_0.getId(), td.TRANSACTION_0.getSenderId(),  (byte) 1, 2000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        POLL_1 = new PhasingPoll(td.TRANSACTION_8.getId(), td.TRANSACTION_8.getSenderId(),  (byte) 1, 10000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        POLL_2 = new PhasingPoll(td.TRANSACTION_7.getId(), td.TRANSACTION_7.getSenderId(),  (byte) 1, 9500, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        POLL_3 = new PhasingPoll(td.TRANSACTION_12.getId(),td.TRANSACTION_12.getSenderId(), (byte) 1, 17000, (byte) 0, 1,0, 0, (byte) 0, null, (byte) 0);
        RESULT_0 = new PhasingPollResult(POLL_0.getId(), 1, true, 1500);
        RESULT_1 = new PhasingPollResult(POLL_1.getId(), 0, true, 4000);
        RESULT_2 = new PhasingPollResult(POLL_2.getId(), 0, false, 9500);
    }
}

