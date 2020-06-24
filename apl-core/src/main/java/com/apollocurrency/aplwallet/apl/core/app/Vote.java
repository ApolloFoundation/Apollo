/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Vote {
    private final long id;
    private final DbKey dbKey;
    private final long pollId;
    private final long voterId;
    private final byte[] voteBytes;

    public Vote(Transaction transaction, MessagingVoteCasting attachment, DbKey dbKey) {
        this.id = transaction.getId();
        this.dbKey = dbKey;
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.voteBytes = attachment.getPollVote();
    }

    public Vote(long id, DbKey dbKey, long pollId, long voterId, byte[] voteBytes) {
        this.id = id;
        this.dbKey = dbKey;
        this.pollId = pollId;
        this.voterId = voterId;
        this.voteBytes = voteBytes;
    }

    public long getId() {
        return id;
    }

    public long getPollId() {
        return pollId;
    }

    public long getVoterId() {
        return voterId;
    }

    public byte[] getVoteBytes() {
        return voteBytes;
    }

    public DbKey getDbKey() {
        return dbKey;
    }
}
