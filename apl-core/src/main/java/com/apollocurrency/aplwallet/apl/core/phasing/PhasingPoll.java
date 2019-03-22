/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import com.apollocurrency.aplwallet.apl.core.app.AbstractPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PhasingPoll extends AbstractPoll  {
    private final long[] whitelist;
    private final long quorum;
    private final byte[] hashedSecret;
    private final byte algorithm;
    private List<byte[]> linkedFullHashes;
    private byte[] fullHash;
    public PhasingPoll(Transaction transaction, PhasingAppendix appendix) {
        super(transaction.getId(), transaction.getSenderId(), appendix.getFinishHeight(), appendix.getVoteWeighting());
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        this.hashedSecret = appendix.getHashedSecret();
        this.algorithm = appendix.getAlgorithm();
    }

    public List<byte[]> getLinkedFullHashes() {
        return linkedFullHashes;
    }

    public void setLinkedFullHashes(List<byte[]> linkedFullHashes) {
        this.linkedFullHashes = linkedFullHashes;
    }

    public void setFullHash(byte[] fullHash) {
        this.fullHash = fullHash;
    }

    public PhasingPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.quorum = rs.getLong("quorum");
        this.whitelist = Convert.EMPTY_LONG;
        this.hashedSecret = rs.getBytes("hashed_secret");
        this.algorithm = rs.getByte("algorithm");
    }

    public PhasingPoll(ResultSet rs, List<Long> whitelist) throws SQLException {
        super(rs);
        this.quorum = rs.getLong("quorum");
        this.whitelist = Convert.toArray(whitelist);
        this.hashedSecret = rs.getBytes("hashed_secret");
        this.algorithm = rs.getByte("algorithm");
    }



    public long[] getWhitelist() {
        return whitelist;
    }

    public long getQuorum() {
        return quorum;
    }

    public byte[] getFullHash() {
        return fullHash;
    }


    public byte[] getHashedSecret() {
        return hashedSecret;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public boolean verifySecret(byte[] revealedSecret) {
        HashFunction hashFunction = PhasingPollService.getHashFunction(algorithm);
        return hashFunction != null && Arrays.equals(hashedSecret, hashFunction.hash(revealedSecret));
    }


    public boolean allowEarlyFinish() {
        return voteWeighting.isBalanceIndependent() && (whitelist.length > 0 || voteWeighting.getVotingModel() != VoteWeighting.VotingModel.ACCOUNT);
    }

}
