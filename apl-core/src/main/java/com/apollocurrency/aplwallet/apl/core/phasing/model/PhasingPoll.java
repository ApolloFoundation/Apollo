/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.app.AbstractPoll;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PhasingPoll extends AbstractPoll {

    private long[] whitelist;
    private int finishTime = -1;
    private final long quorum;
    private final byte[] hashedSecret;
    private final byte algorithm;
    private byte[][] linkedFullHashes;
    private byte[] fullHash;

    public List<byte[]> getLinkedFullHashes() {
        return linkedFullHashes == null ? null : Arrays.asList(linkedFullHashes);
    }

    public void setLinkedFullHashes(List<byte[]> linkedFullHashes) {
        if (linkedFullHashes != null) {
            this.linkedFullHashes =  linkedFullHashes.toArray(Convert.EMPTY_BYTES);
        } else {
            this.linkedFullHashes = null;
        }
    }

    public void setFullHash(byte[] fullHash) {
        this.fullHash = fullHash;
    }

/*
    public PhasingPoll(ResultSet rs) throws SQLException {
        super(rs);
        this.finishTime = rs.getInt("finish_time");
        this.quorum = rs.getLong("quorum");
        this.whitelist = rs.getByte("whitelist_size") == 0 ? Convert.EMPTY_LONG : null;
        this.hashedSecret = rs.getBytes("hashed_secret");
        this.algorithm = rs.getByte("algorithm");
    }

    public PhasingPoll(Transaction transaction, PhasingAppendix appendix) { // replaced by - PhasingCreator.createPoll(transaction, appendix);
        super(transaction.getDbId(), transaction.getHeight(), transaction.getId(),
                new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel),
                transaction.getSenderId(), appendix.getFinishHeight()*/
/*, appendix.getVoteWeighting()*//*
);
        this.quorum = appendix.getQuorum();
        if(appendix instanceof PhasingAppendixV2) {
            this.finishTime = ((PhasingAppendixV2)appendix).getFinishTime();
        }
        this.whitelist = appendix.getWhitelist();
        this.hashedSecret = appendix.getHashedSecret();
        this.algorithm = appendix.getAlgorithm();
        this.fullHash = transaction.getFullHash();
        this.linkedFullHashes = appendix.getLinkedFullHashes();
    }
*/

    public PhasingPoll(Long dbId, long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight,
                       long quorum, VoteWeighting voteWeighting, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes, Integer height) {
        super(dbId, height, id, voteWeighting, accountId, finishHeight);
        this.whitelist = whitelist;
        this.fullHash = fullHash;
        this.quorum = quorum;
        this.hashedSecret = hashedSecret;
        this.algorithm = algorithm;
        this.linkedFullHashes = linkedFullhashes;
    }
/*
    public PhasingPoll(long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, int finishTime, byte votingModel,long quorum,
                       long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes) {
        super(null, null, id, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), accountId, finishHeight);
        this.finishTime = finishTime;
        this.whitelist = whitelist == null ? Convert.EMPTY_LONG : whitelist;
        this.fullHash = fullHash;
        this.quorum = quorum;
        this.hashedSecret = hashedSecret;
        this.algorithm = algorithm;
        this.linkedFullHashes =linkedFullhashes != null ? linkedFullhashes : Convert.EMPTY_BYTES;
    }
*/

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

    public int getFinishTime() {
        return finishTime;
    }

/*
    public boolean verifySecret(byte[] revealedSecret) {
        HashFunction hashFunction = PhasingPollService.getHashFunction(algorithm);
        return hashFunction != null && Arrays.equals(hashedSecret, hashFunction.hash(revealedSecret));
    }
*/

    public void setWhitelist(long[] whitelist) {
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
        int result = Objects.hash(super.hashCode(), quorum, algorithm);
        result = 31 * result + Arrays.hashCode(whitelist);
        result = 31 * result + Arrays.hashCode(hashedSecret);
        result = 31 * result + Arrays.deepHashCode(linkedFullHashes);
        result = 31 * result + Arrays.hashCode(fullHash);
        return result;
    }

    public boolean allowEarlyFinish() {
        return voteWeighting.isBalanceIndependent() && (whitelist.length > 0 || voteWeighting.getVotingModel() != VoteWeighting.VotingModel.ACCOUNT);
    }
}
