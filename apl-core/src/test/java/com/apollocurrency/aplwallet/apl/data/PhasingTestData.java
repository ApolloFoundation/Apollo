/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingCreator;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

public class PhasingTestData {
    public final long POLL_1_VOTER_0_ID = 5564664969772495473L;
    public final long POLL_1_VOTER_1_ID = -8315839810807014152L;
    public final long POLL_4_VOTER_0_ID = -8315839810807014152L;
    public final PhasingPollVoter POLL_1_VOTER_0;
    public final PhasingPollVoter POLL_1_VOTER_1;
    public final PhasingPollVoter POLL_4_VOTER_0;
    public final PhasingPollVoter NEW_VOTER_0;
    public final PhasingPollVoter NEW_VOTER_1;
    public final PhasingPollVoter NEW_VOTER_2;
    public final int NUMBER_OF_PHASED_TRANSACTIONS = 6;
    public final byte[] LINKED_TRANSACTION_0_HASH = Convert.parseHexString("6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e");
    public final byte[] LINKED_TRANSACTION_1_HASH;
    public final byte[] LINKED_TRANSACTION_2_HASH;

    public final PhasingPollLinkedTransaction LINKED_TRANSACTION_0;
    public final PhasingPollLinkedTransaction LINKED_TRANSACTION_1;
    public final PhasingPollLinkedTransaction LINKED_TRANSACTION_2;
    public final PhasingPollLinkedTransaction NEW_LINKED_TRANSACTION_1;
    public final PhasingPollLinkedTransaction NEW_LINKED_TRANSACTION_2;
    public final PhasingPollLinkedTransaction NEW_LINKED_TRANSACTION_3;

    private final TransactionTestData td;
    public final PhasingPoll POLL_1;
    public final PhasingPoll POLL_2;
    public final PhasingPoll POLL_3;
    public final PhasingPoll POLL_4;
    public final PhasingPollResult SHARD_RESULT_0;
    public final PhasingPollResult RESULT_0;
    public final PhasingPollResult RESULT_1;
    public final PhasingPollResult RESULT_2;
    public final PhasingVote POLL_1_VOTE_0;
    public final PhasingVote POLL_1_VOTE_1;
    public final PhasingPoll NEW_POLL;
    public final PhasingParams NEW_POLL_PARAMS;
    public final PhasingAppendix NEW_POLL_APPENDIX;
    public final Transaction NEW_VOTE_TX;
    public final PhasingVote NEW_VOTE;
    public final PhasingVote NEW_LINKED_TX_VOTE;

    public PhasingTestData() {
        td = new TransactionTestData();
        LINKED_TRANSACTION_1_HASH = td.TRANSACTION_11.getFullHash();
        LINKED_TRANSACTION_2_HASH = td.NOT_SAVED_TRANSACTION.getFullHash();
        POLL_1 = PhasingCreator.createPoll(20, td.TRANSACTION_8.getId(), td.TRANSACTION_8.getSenderId(), new long[] {POLL_1_VOTER_0_ID, POLL_1_VOTER_1_ID}, td.TRANSACTION_8.getFullHash(), 10000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0, null, td.TRANSACTION_8.getHeight());
        POLL_2 = PhasingCreator.createPoll(30, td.TRANSACTION_7.getId(), td.TRANSACTION_7.getSenderId(), null, td.TRANSACTION_7.getFullHash(), 9500, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0, null, td.TRANSACTION_7.getHeight());
        POLL_3 = PhasingCreator.createPoll(40, td.TRANSACTION_12.getId(), td.TRANSACTION_12.getSenderId(), null, td.TRANSACTION_12.getFullHash(), 17000, (byte) 4, 3, 0, 0, (byte) 0, null, (byte) 0, new byte[][]{LINKED_TRANSACTION_0_HASH, LINKED_TRANSACTION_1_HASH, LINKED_TRANSACTION_2_HASH}, td.TRANSACTION_12.getHeight());
        POLL_4 = PhasingCreator.createPoll(50, td.TRANSACTION_11.getId(), td.TRANSACTION_11.getSenderId(), new long[]{POLL_4_VOTER_0_ID}, td.TRANSACTION_11.getFullHash(), 18000, (byte) 0, 1, 0, 0, (byte) 0, null, (byte) 0, null, td.TRANSACTION_11.getHeight());
        POLL_1_VOTER_0 = new PhasingPollVoter(20L, POLL_1.getHeight(), POLL_1.getId(), POLL_1_VOTER_0_ID);
        POLL_1_VOTER_1 = new PhasingPollVoter(30L,POLL_1.getHeight(), POLL_1.getId(), POLL_1_VOTER_1_ID);
        POLL_4_VOTER_0 = new PhasingPollVoter(40L, POLL_4.getHeight(), POLL_4.getId(), POLL_4_VOTER_0_ID);
        SHARD_RESULT_0 = new PhasingPollResult(10L, 300, 100, 1, true);
        RESULT_0 = new PhasingPollResult(20L, 1500, td.TRANSACTION_0.getId(), 1, true);
        RESULT_1 = new PhasingPollResult(30L, 9000, POLL_1.getId(), 0, true);
        RESULT_2 = new PhasingPollResult(40L, 9500, POLL_2.getId(), 0, false );
        POLL_1_VOTE_0 = new PhasingVote(30L, 8500, POLL_1.getId(), POLL_1_VOTER_0_ID, td.TRANSACTION_9.getId());
        POLL_1_VOTE_1 = new PhasingVote(40L, 8999, POLL_1.getId(), POLL_1_VOTER_1_ID, td.TRANSACTION_10.getId());
        LINKED_TRANSACTION_0 = new PhasingPollLinkedTransaction(10L, td.TRANSACTION_12.getHeight(), POLL_3.getId(), Convert.fullHashToId(LINKED_TRANSACTION_0_HASH), LINKED_TRANSACTION_0_HASH);
        LINKED_TRANSACTION_1 = new PhasingPollLinkedTransaction(20L, td.TRANSACTION_12.getHeight(), POLL_3.getId(), Convert.fullHashToId(LINKED_TRANSACTION_1_HASH), LINKED_TRANSACTION_1_HASH);
        LINKED_TRANSACTION_2 = new PhasingPollLinkedTransaction(30L, td.TRANSACTION_12.getHeight(), POLL_3.getId(), Convert.fullHashToId(LINKED_TRANSACTION_2_HASH), LINKED_TRANSACTION_2_HASH);
        NEW_POLL_PARAMS     = new PhasingParams((byte) 0, 0, 3, 0, (byte)0, new long[] {1, 2, 3});
        NEW_POLL_APPENDIX = new PhasingAppendix(20000, NEW_POLL_PARAMS, new byte[][] {td.TRANSACTION_4.getFullHash(), td.TRANSACTION_5.getFullHash()}, null, (byte) 0);
        NEW_POLL = PhasingCreator.createPoll(td.TRANSACTION_10, NEW_POLL_APPENDIX);
        NEW_POLL.setDbId(POLL_4.getDbId() + 1);
        NEW_VOTE_TX = td.TRANSACTION_3;
        NEW_VOTE = new PhasingVote(POLL_1_VOTE_1.getDbId() + 1,  NEW_VOTE_TX.getHeight(), POLL_1.getId(), NEW_VOTE_TX.getSenderId(), NEW_VOTE_TX.getId());
        NEW_LINKED_TX_VOTE = new PhasingVote(POLL_1_VOTE_1.getDbId() + 1, td.NOT_SAVED_TRANSACTION.getHeight(), POLL_3.getId(), td.NOT_SAVED_TRANSACTION.getSenderId(), td.NOT_SAVED_TRANSACTION.getId());
        NEW_LINKED_TRANSACTION_1 = new PhasingPollLinkedTransaction(LINKED_TRANSACTION_2.getDbId() + 1, td.TRANSACTION_8.getHeight(), POLL_1.getId(), td.TRANSACTION_0.getId(), td.TRANSACTION_0.getFullHash());
        NEW_LINKED_TRANSACTION_2 = new PhasingPollLinkedTransaction(LINKED_TRANSACTION_2.getDbId() + 2, td.TRANSACTION_8.getHeight(), POLL_1.getId(), td.TRANSACTION_2.getId(), td.TRANSACTION_2.getFullHash());
        NEW_LINKED_TRANSACTION_3 = new PhasingPollLinkedTransaction(LINKED_TRANSACTION_2.getDbId() + 3, td.TRANSACTION_11.getHeight(), POLL_4.getId(), td.TRANSACTION_3.getId(), td.TRANSACTION_3.getFullHash());
        NEW_VOTER_0 = new PhasingPollVoter(POLL_4_VOTER_0.getDbId() + 1, POLL_4.getHeight(), POLL_4.getId(), POLL_1_VOTER_0_ID);
        NEW_VOTER_1 = new PhasingPollVoter(POLL_4_VOTER_0.getDbId() + 2, POLL_4.getHeight(), POLL_4.getId(), 10000L);
        NEW_VOTER_2 = new PhasingPollVoter(POLL_4_VOTER_0.getDbId() + 3, POLL_2.getHeight(), POLL_2.getId(), 20000L);
    }
}

