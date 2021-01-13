/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class for handling phasing parameters
 */
public final class PhasingParams {

    private final long quorum;
    private final long[] whitelist;
    private final VoteWeighting voteWeighting;

    public PhasingParams(ByteBuffer buffer) {
        byte votingModel = buffer.get();
        quorum = buffer.getLong();
        long minBalance = buffer.getLong();
        byte whitelistSize = buffer.get();
        if (whitelistSize > 0) {
            whitelist = new long[whitelistSize];
            for (int i = 0; i < whitelistSize; i++) {
                whitelist[i] = buffer.getLong();
            }
        } else {
            whitelist = Convert.EMPTY_LONG;
        }
        long holdingId = buffer.getLong();
        byte minBalanceModel = buffer.get();
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }

    public PhasingParams(Map<?,?> attachmentData) {
        quorum = Convert.parseLong(attachmentData.get("phasingQuorum"));
        long minBalance = Convert.parseLong(attachmentData.get("phasingMinBalance"));
        byte votingModel = ((Number) attachmentData.get("phasingVotingModel")).byteValue();
        long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("phasingHolding"));
        List<?> whitelistJson = (List<?>) (attachmentData.get("phasingWhitelist"));
        if (whitelistJson != null && whitelistJson.size() > 0) {
            whitelist = new long[whitelistJson.size()];
            for (int i = 0; i < whitelist.length; i++) {
                whitelist[i] = Convert.parseUnsignedLong((String) whitelistJson.get(i));
            }
        } else {
            whitelist = Convert.EMPTY_LONG;
        }
        byte minBalanceModel = ((Number) attachmentData.get("phasingMinBalanceModel")).byteValue();
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }

    public PhasingParams(byte votingModel, long holdingId, long quorum, long minBalance, byte minBalanceModel, long[] whitelist) {
        this.quorum = quorum;
        this.whitelist = Convert.nullToEmpty(whitelist);
        if (this.whitelist.length > 0) {
            Arrays.sort(this.whitelist);
        }
        voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
    }

    public int getMySize() {
        return 1 + 8 + 8 + 1 + 8 * whitelist.length + 8 + 1;
    }

    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(voteWeighting.getVotingModel().getCode());
        buffer.putLong(quorum);
        buffer.putLong(voteWeighting.getMinBalance());
        buffer.put((byte) whitelist.length);
        for (long account : whitelist) {
            buffer.putLong(account);
        }
        buffer.putLong(voteWeighting.getHoldingId());
        buffer.put(voteWeighting.getMinBalanceModel().getCode());
    }

    public void putMyJSON(JSONObject json) {
        json.put("phasingQuorum", quorum);
        json.put("phasingMinBalance", voteWeighting.getMinBalance());
        json.put("phasingVotingModel", voteWeighting.getVotingModel().getCode());
        json.put("phasingHolding", Long.toUnsignedString(voteWeighting.getHoldingId()));
        json.put("phasingMinBalanceModel", voteWeighting.getMinBalanceModel().getCode());
        if (whitelist.length > 0) {
            JSONArray whitelistJson = new JSONArray();
            for (long accountId : whitelist) {
                whitelistJson.add(Long.toUnsignedString(accountId));
            }
            json.put("phasingWhitelist", whitelistJson);
        }
    }

    public long getQuorum() {
        return quorum;
    }

    public long[] getWhitelist() {
        return whitelist;
    }

    public VoteWeighting getVoteWeighting() {
        return voteWeighting;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhasingParams)) {
            return false;
        }
        PhasingParams other = (PhasingParams) obj;
        return other.quorum == this.quorum
            && other.voteWeighting.equals(this.voteWeighting)
            && Arrays.equals(other.whitelist, this.whitelist);
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode = 31 * hashCode + Long.hashCode(quorum);
        for (long voter : whitelist) {
            hashCode = 31 * hashCode + Long.hashCode(voter);
        }
        hashCode = 31 * hashCode + voteWeighting.hashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        JSONObject resultJson = new JSONObject();
        putMyJSON(resultJson);
        return resultJson.toJSONString();
    }
}
