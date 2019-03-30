/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.app.AbstractPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PhasingPoll extends AbstractPoll {

    private long[] whitelist;
    private final long quorum;
    private final byte[] hashedSecret;
    private final byte algorithm;
    private byte[][] linkedFullHashes;
    private byte[] fullHash;

    public PhasingPoll(Transaction transaction, PhasingAppendix appendix) {
        super(transaction.getId(), transaction.getSenderId(), appendix.getFinishHeight(), appendix.getVoteWeighting());
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        this.hashedSecret = appendix.getHashedSecret();
        this.algorithm = appendix.getAlgorithm();
        this.fullHash = transaction.getFullHash();
        this.linkedFullHashes = appendix.getLinkedFullHashes();
    }

    public List<byte[]> getLinkedFullHashes() {
        return linkedFullHashes == null ? null : Arrays.asList(linkedFullHashes);
    }

    public void setLinkedFullHashes(List<byte[]> linkedFullHashes) {
        this.linkedFullHashes =  linkedFullHashes.toArray(Convert.EMPTY_BYTES);
    }

    public void setFullHash(byte[] fullHash) {
        this.fullHash = fullHash;
    }

    public PhasingPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.quorum = rs.getLong("quorum");
        this.whitelist = rs.getByte("whitelist_size") == 0 ? Convert.EMPTY_LONG : null;
        this.hashedSecret = rs.getBytes("hashed_secret");
        this.algorithm = rs.getByte("algorithm");
    }

    public PhasingPoll(long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, byte votingModel,long quorum,
                       long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes) {
        super(id, accountId, finishHeight, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel));
        this.whitelist = whitelist == null ? Convert.EMPTY_LONG : whitelist;
        this.fullHash = fullHash;
        this.quorum = quorum;
        this.hashedSecret = hashedSecret;
        this.algorithm = algorithm;
        this.linkedFullHashes =linkedFullhashes != null ? linkedFullhashes : Convert.EMPTY_BYTES;
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

    public void setWhitelist(long[] whitelist) {
        Objects.requireNonNull(whitelist, "Whitelist should not be null");
        this.whitelist = whitelist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhasingPoll)) return false;
        if (!super.equals(o)) return false;
        PhasingPoll that = (PhasingPoll) o;
        return quorum == that.quorum &&
                algorithm == that.algorithm &&
                Arrays.equals(whitelist, that.whitelist) &&
                Arrays.equals(hashedSecret, that.hashedSecret) &&
                Arrays.deepEquals(linkedFullHashes, that.linkedFullHashes) &&
                Arrays.equals(fullHash, that.fullHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), quorum, algorithm, linkedFullHashes);
        result = 31 * result + Arrays.hashCode(whitelist);
        result = 31 * result + Arrays.hashCode(hashedSecret);
        result = 31 * result + Arrays.hashCode(fullHash);
        return result;
    }

    public boolean allowEarlyFinish() {
        return voteWeighting.isBalanceIndependent() && (whitelist.length > 0 || voteWeighting.getVotingModel() != VoteWeighting.VotingModel.ACCOUNT);
    }

}
