/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.app.AbstractPoll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PhasingPoll extends AbstractPoll {

    private DbKey dbKey;
    private long[] whitelist;
    private final long quorum;
    private final byte[] hashedSecret;
    private final byte algorithm;
    private byte[][] linkedFullHashes;
    private byte[] fullHash;
    private int height;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public PhasingPoll(Transaction transaction, PhasingAppendix appendix, int height) {
        super(transaction.getId(), transaction.getSenderId(), appendix.getFinishHeight(), appendix.getVoteWeighting());
        this.quorum = appendix.getQuorum();
        this.whitelist = appendix.getWhitelist();
        this.hashedSecret = appendix.getHashedSecret();
        this.algorithm = appendix.getAlgorithm();
        this.fullHash = transaction.getFullHash();
        this.linkedFullHashes = appendix.getLinkedFullHashes();
        this.height = height;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
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

    public PhasingPoll(long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, byte votingModel,long quorum,
                       long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes, int height) {
        super(id, accountId, finishHeight, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel));
        this.whitelist = whitelist == null ? Convert.EMPTY_LONG : whitelist;
        this.fullHash = fullHash;
        this.quorum = quorum;
        this.hashedSecret = hashedSecret;
        this.algorithm = algorithm;
        this.linkedFullHashes =linkedFullhashes != null ? linkedFullhashes : Convert.EMPTY_BYTES;
        this.height = height;
    }

    public PhasingPoll(long id, long accountId, int finishHeight, byte votingModel, long quorum,
                       long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, int height) {
        super(id, accountId, finishHeight, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel));
        this.quorum = quorum;
        this.hashedSecret = hashedSecret;
        this.algorithm = algorithm;
        this.height = height;
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
                height == that.height &&
                Arrays.equals(whitelist, that.whitelist) &&
                Arrays.equals(hashedSecret, that.hashedSecret) &&
                Arrays.deepEquals(linkedFullHashes, that.linkedFullHashes) &&
                Arrays.equals(fullHash, that.fullHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), quorum, algorithm, height);
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
