/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;

public class PhasingTestData {
    public final long POLL_1_VOTER = 5564664969772495473L;
    public final long POLL_3_VOTER = 5564664969772495473L;
    public final int NUMBER_OF_PHASED_TRANSACTIONS = 5;

    private final TransactionTestData td;
    public final PhasingPoll POLL_1;
    public final PhasingPoll POLL_2;
    public final PhasingPoll POLL_3;
    public final PhasingPollResult SHARD_RESULT_0;
    public final PhasingPollResult RESULT_0;
    public final PhasingPollResult RESULT_1;
    public final PhasingPollResult RESULT_2;
    public final PhasingVote POLL_1_VOTE_0;
    public final PhasingVote POLL_3_VOTE_0;
    public final PhasingPoll NEW_POLL;
    public final PhasingParams NEW_POLL_PARAMS;
    public final PhasingAppendix NEW_POLL_APPENDIX;
    public final Transaction NEW_VOTE_TX;
    public final PhasingVote NEW_VOTE;

    public PhasingTestData() {
        td = new TransactionTestData();
        POLL_1 = new PhasingPoll(td.TRANSACTION_8.getId(), td.TRANSACTION_8.getSenderId(), new long[] {POLL_1_VOTER}, td.TRANSACTION_8.getFullHash(), 10000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        POLL_2 = new PhasingPoll(td.TRANSACTION_7.getId(), td.TRANSACTION_7.getSenderId(), null, td.TRANSACTION_7.getFullHash(), 9500, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        POLL_3 = new PhasingPoll(td.TRANSACTION_12.getId(), td.TRANSACTION_12.getSenderId(), new long[] {POLL_3_VOTER}, td.TRANSACTION_12.getFullHash(), 17000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0);
        SHARD_RESULT_0 = new PhasingPollResult(100, 1, true, 300);
        RESULT_0 = new PhasingPollResult(td.TRANSACTION_0.getId(), 1, true, 1500);
        RESULT_1 = new PhasingPollResult(POLL_1.getId(), 0, true, 9000);
        RESULT_2 = new PhasingPollResult(POLL_2.getId(), 0, false, 9500);
        POLL_1_VOTE_0 = new PhasingVote(POLL_1.getId(), POLL_1_VOTER, td.TRANSACTION_9.getId());
        POLL_3_VOTE_0 = new PhasingVote(POLL_3.getId(), POLL_3_VOTER, td.TRANSACTION_10.getId());
        NEW_POLL_PARAMS     = new PhasingParams((byte) 0, 0, 3, 0, (byte)0, new long[] {1, 2, 3});
        NEW_POLL_APPENDIX = new PhasingAppendix(20000, NEW_POLL_PARAMS, new byte[][] {td.TRANSACTION_4.getFullHash(), td.TRANSACTION_5.getFullHash()}, null, (byte) 0);
        NEW_POLL = new PhasingPoll(td.TRANSACTION_11, NEW_POLL_APPENDIX);
        NEW_VOTE_TX = td.TRANSACTION_10;
        NEW_VOTE = new PhasingVote(POLL_3.getId(), NEW_VOTE_TX.getSenderId(), NEW_VOTE_TX.getId());
    }
}

