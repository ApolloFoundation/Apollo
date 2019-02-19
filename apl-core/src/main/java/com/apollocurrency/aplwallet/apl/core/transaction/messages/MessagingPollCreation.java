/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MessagingPollCreation extends AbstractAttachment {
    
    public static final class PollBuilder {

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;
        private final int finishHeight;
        private final byte votingModel;
        private long minBalance = 0;
        private byte minBalanceModel;
        private final byte minNumberOfOptions;
        private final byte maxNumberOfOptions;
        private final byte minRangeValue;
        private final byte maxRangeValue;
        private long holdingId;

        public PollBuilder(final String pollName, final String pollDescription, final String[] pollOptions, final int finishHeight, final byte votingModel, byte minNumberOfOptions, byte maxNumberOfOptions, byte minRangeValue, byte maxRangeValue) {
            this.pollName = pollName;
            this.pollDescription = pollDescription;
            this.pollOptions = pollOptions;
            this.finishHeight = finishHeight;
            this.votingModel = votingModel;
            this.minNumberOfOptions = minNumberOfOptions;
            this.maxNumberOfOptions = maxNumberOfOptions;
            this.minRangeValue = minRangeValue;
            this.maxRangeValue = maxRangeValue;
            this.minBalanceModel = VoteWeighting.VotingModel.get(votingModel).getMinBalanceModel().getCode();
        }

        public PollBuilder minBalance(byte minBalanceModel, long minBalance) {
            this.minBalanceModel = minBalanceModel;
            this.minBalance = minBalance;
            return this;
        }

        public PollBuilder holdingId(long holdingId) {
            this.holdingId = holdingId;
            return this;
        }

        public MessagingPollCreation build() {
            return new MessagingPollCreation(this);
        }
    }
    final String pollName;
    final String pollDescription;
    final String[] pollOptions;
    final int finishHeight;
    final byte minNumberOfOptions;
    final byte maxNumberOfOptions;
    final byte minRangeValue;
    final byte maxRangeValue;
    final VoteWeighting voteWeighting;

    public MessagingPollCreation(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.pollName = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
            this.pollDescription = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);
            this.finishHeight = buffer.getInt();
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new AplException.NotValidException("Invalid number of poll options: " + numberOfOptions);
            }
            this.pollOptions = new String[numberOfOptions];
            for (int i = 0; i < numberOfOptions; i++) {
                this.pollOptions[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
            }
            byte votingModel = buffer.get();
            this.minNumberOfOptions = buffer.get();
            this.maxNumberOfOptions = buffer.get();
            this.minRangeValue = buffer.get();
            this.maxRangeValue = buffer.get();
            long minBalance = buffer.getLong();
            byte minBalanceModel = buffer.get();
            long holdingId = buffer.getLong();
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public MessagingPollCreation(JSONObject attachmentData) {
        super(attachmentData);
        this.pollName = ((String) attachmentData.get("name")).trim();
        this.pollDescription = ((String) attachmentData.get("description")).trim();
        this.finishHeight = ((Long) attachmentData.get("finishHeight")).intValue();
        JSONArray options = (JSONArray) attachmentData.get("options");
        this.pollOptions = new String[options.size()];
        for (int i = 0; i < pollOptions.length; i++) {
            this.pollOptions[i] = ((String) options.get(i)).trim();
        }
        byte votingModel = ((Long) attachmentData.get("votingModel")).byteValue();
        this.minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
        this.maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
        this.minRangeValue = ((Long) attachmentData.get("minRangeValue")).byteValue();
        this.maxRangeValue = ((Long) attachmentData.get("maxRangeValue")).byteValue();
        long minBalance = Convert.parseLong(attachmentData.get("minBalance"));
        byte minBalanceModel = ((Long) attachmentData.get("minBalanceModel")).byteValue();
        long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
        this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }

    private MessagingPollCreation(PollBuilder builder) {
        this.pollName = builder.pollName;
        this.pollDescription = builder.pollDescription;
        this.pollOptions = builder.pollOptions;
        this.finishHeight = builder.finishHeight;
        this.minNumberOfOptions = builder.minNumberOfOptions;
        this.maxNumberOfOptions = builder.maxNumberOfOptions;
        this.minRangeValue = builder.minRangeValue;
        this.maxRangeValue = builder.maxRangeValue;
        this.voteWeighting = new VoteWeighting(builder.votingModel, builder.holdingId, builder.minBalance, builder.minBalanceModel);
    }

    @Override
    int getMySize() {
        int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
        for (String pollOption : pollOptions) {
            size += 2 + Convert.toBytes(pollOption).length;
        }
        size += 4 + 1 + 1 + 1 + 1 + 1 + 8 + 1 + 8;
        return size;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] name = Convert.toBytes(this.pollName);
        byte[] description = Convert.toBytes(this.pollDescription);
        byte[][] options = new byte[this.pollOptions.length][];
        for (int i = 0; i < this.pollOptions.length; i++) {
            options[i] = Convert.toBytes(this.pollOptions[i]);
        }
        buffer.putShort((short) name.length);
        buffer.put(name);
        buffer.putShort((short) description.length);
        buffer.put(description);
        buffer.putInt(finishHeight);
        buffer.put((byte) options.length);
        for (byte[] option : options) {
            buffer.putShort((short) option.length);
            buffer.put(option);
        }
        buffer.put(this.voteWeighting.getVotingModel().getCode());
        buffer.put(this.minNumberOfOptions);
        buffer.put(this.maxNumberOfOptions);
        buffer.put(this.minRangeValue);
        buffer.put(this.maxRangeValue);
        buffer.putLong(this.voteWeighting.getMinBalance());
        buffer.put(this.voteWeighting.getMinBalanceModel().getCode());
        buffer.putLong(this.voteWeighting.getHoldingId());
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("name", this.pollName);
        attachment.put("description", this.pollDescription);
        attachment.put("finishHeight", this.finishHeight);
        JSONArray options = new JSONArray();
        if (this.pollOptions != null) {
            Collections.addAll(options, this.pollOptions);
        }
        attachment.put("options", options);
        attachment.put("minNumberOfOptions", this.minNumberOfOptions);
        attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);
        attachment.put("minRangeValue", this.minRangeValue);
        attachment.put("maxRangeValue", this.maxRangeValue);
        attachment.put("votingModel", this.voteWeighting.getVotingModel().getCode());
        attachment.put("minBalance", this.voteWeighting.getMinBalance());
        attachment.put("minBalanceModel", this.voteWeighting.getMinBalanceModel().getCode());
        attachment.put("holding", Long.toUnsignedString(this.voteWeighting.getHoldingId()));
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.POLL_CREATION;
    }

    public String getPollName() {
        return pollName;
    }

    public String getPollDescription() {
        return pollDescription;
    }

    public int getFinishHeight() {
        return finishHeight;
    }

    public String[] getPollOptions() {
        return pollOptions;
    }

    public byte getMinNumberOfOptions() {
        return minNumberOfOptions;
    }

    public byte getMaxNumberOfOptions() {
        return maxNumberOfOptions;
    }

    public byte getMinRangeValue() {
        return minRangeValue;
    }

    public byte getMaxRangeValue() {
        return maxRangeValue;
    }

    public VoteWeighting getVoteWeighting() {
        return voteWeighting;
    }
    
}
