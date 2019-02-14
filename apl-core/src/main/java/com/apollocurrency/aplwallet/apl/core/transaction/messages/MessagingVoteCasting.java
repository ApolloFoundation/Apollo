/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MessagingVoteCasting extends AbstractAttachment {
    
    final long pollId;
    final byte[] pollVote;

    public MessagingVoteCasting(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        pollId = buffer.getLong();
        int numberOfOptions = buffer.get();
        if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
            throw new AplException.NotValidException("More than " + Constants.MAX_POLL_OPTION_COUNT + " options in a vote");
        }
        pollVote = new byte[numberOfOptions];
        buffer.get(pollVote);
    }

    public MessagingVoteCasting(JSONObject attachmentData) {
        super(attachmentData);
        pollId = Convert.parseUnsignedLong((String) attachmentData.get("poll"));
        JSONArray vote = (JSONArray) attachmentData.get("vote");
        pollVote = new byte[vote.size()];
        for (int i = 0; i < pollVote.length; i++) {
            pollVote[i] = ((Long) vote.get(i)).byteValue();
        }
    }

    public MessagingVoteCasting(long pollId, byte[] pollVote) {
        this.pollId = pollId;
        this.pollVote = pollVote;
    }

    @Override
    int getMySize() {
        return 8 + 1 + this.pollVote.length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.pollId);
        buffer.put((byte) this.pollVote.length);
        buffer.put(this.pollVote);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("poll", Long.toUnsignedString(this.pollId));
        JSONArray vote = new JSONArray();
        if (this.pollVote != null) {
            for (byte aPollVote : this.pollVote) {
                vote.add(aPollVote);
            }
        }
        attachment.put("vote", vote);
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.VOTE_CASTING;
    }

    public long getPollId() {
        return pollId;
    }

    public byte[] getPollVote() {
        return pollVote;
    }
    
}
