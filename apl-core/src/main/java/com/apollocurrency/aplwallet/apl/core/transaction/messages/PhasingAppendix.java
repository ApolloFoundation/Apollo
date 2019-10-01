/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;

public class PhasingAppendix extends AbstractAppendix {
    private static final Logger LOG = getLogger(PhasingAppendix.class);
    private static Blockchain blockchain;// = CDI.current().select(Blockchain.class).get();
    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    private static final Fee PHASING_FEE = (transaction, appendage) -> {
        long fee = 0;
        PhasingAppendix phasing = (PhasingAppendix)appendage;
        if (!phasing.params.getVoteWeighting().isBalanceIndependent()) {
            fee += 20 * Constants.ONE_APL;
        } else {
            fee += Constants.ONE_APL;
        }
        if (phasing.hashedSecret.length > 0) {
            fee += (1 + (phasing.hashedSecret.length - 1) / 32) * Constants.ONE_APL;
        }
        fee += Constants.ONE_APL * phasing.linkedFullHashes.length;
        return fee;
    };

    private final int finishHeight;
    private final PhasingParams params;
    private final byte[][] linkedFullHashes;
    private final byte[] hashedSecret;
    private final byte algorithm;


    public PhasingAppendix(ByteBuffer buffer) {
        finishHeight = buffer.getInt();
        params = new PhasingParams(buffer);

        byte linkedFullHashesSize = buffer.get();
        if (linkedFullHashesSize > 0) {
            linkedFullHashes = new byte[linkedFullHashesSize][];
            for (int i = 0; i < linkedFullHashesSize; i++) {
                linkedFullHashes[i] = new byte[32];
                buffer.get(linkedFullHashes[i]);
            }
        } else {
            linkedFullHashes = Convert.EMPTY_BYTES;
        }
        byte hashedSecretLength = buffer.get();
        if (hashedSecretLength > 0) {
            hashedSecret = new byte[hashedSecretLength];
            buffer.get(hashedSecret);
        } else {
            hashedSecret = Convert.EMPTY_BYTE;
        }
        algorithm = buffer.get();
    }

    public PhasingAppendix(JSONObject attachmentData) {
        super(attachmentData);
        Long phasingFinishHeight = (Long) attachmentData.get("phasingFinishHeight");

        this.finishHeight = phasingFinishHeight != null ? phasingFinishHeight.intValue() : -1;
        params = new PhasingParams(attachmentData);
        JSONArray linkedFullHashesJson = (JSONArray) attachmentData.get("phasingLinkedFullHashes");
        if (linkedFullHashesJson != null && linkedFullHashesJson.size() > 0) {
            linkedFullHashes = new byte[linkedFullHashesJson.size()][];
            for (int i = 0; i < linkedFullHashes.length; i++) {
                linkedFullHashes[i] = Convert.parseHexString((String) linkedFullHashesJson.get(i));
            }
        } else {
            linkedFullHashes = Convert.EMPTY_BYTES;
        }
        String hashedSecret = Convert.emptyToNull((String)attachmentData.get("phasingHashedSecret"));
        if (hashedSecret != null) {
            this.hashedSecret = Convert.parseHexString(hashedSecret);
            this.algorithm = ((Long) attachmentData.get("phasingHashedSecretAlgorithm")).byteValue();
        } else {
            this.hashedSecret = Convert.EMPTY_BYTE;
            this.algorithm = 0;
        }
    }

    public PhasingAppendix(int finishHeight, PhasingParams phasingParams, byte[][] linkedFullHashes, byte[] hashedSecret, byte algorithm) {
        this.finishHeight = finishHeight;
        this.params = phasingParams;
        this.linkedFullHashes = Convert.nullToEmpty(linkedFullHashes);
        this.hashedSecret = hashedSecret != null ? hashedSecret : Convert.EMPTY_BYTE;
        this.algorithm = algorithm;
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public String getAppendixName() {
        return "Phasing";
    }

    //TODO think it over how to change it (magic numbers).
    @Override
    public byte getVersion() {
        return Byte.valueOf("1");
    }

    @Override
    public int getMySize() {
        return 4 + params.getMySize() + 1 + 32 * linkedFullHashes.length + 1 + hashedSecret.length + 1;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(finishHeight);
        params.putMyBytes(buffer);
        buffer.put((byte) linkedFullHashes.length);
        for (byte[] hash : linkedFullHashes) {
            buffer.put(hash);
        }
        buffer.put((byte)hashedSecret.length);
        buffer.put(hashedSecret);
        buffer.put(algorithm);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("phasingFinishHeight", finishHeight);
        params.putMyJSON(json);
        if (linkedFullHashes.length > 0) {
            JSONArray linkedFullHashesJson = new JSONArray();
            for (byte[] hash : linkedFullHashes) {
                linkedFullHashesJson.add(Convert.toHexString(hash));
            }
            json.put("phasingLinkedFullHashes", linkedFullHashesJson);
        }
        if (hashedSecret.length > 0) {
            json.put("phasingHashedSecret", Convert.toHexString(hashedSecret));
            json.put("phasingHashedSecretAlgorithm", algorithm);
        }
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        generalValidation(transaction);

        validateFinishHeight(this.finishHeight);
    }

    public void generalValidation(Transaction transaction) throws AplException.ValidationException{
        params.validate();

        lookupBlockchain();

        int currentHeight = blockchain.getHeight();
        if (params.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.TRANSACTION) {
            if (linkedFullHashes.length == 0 || linkedFullHashes.length > Constants.MAX_PHASING_LINKED_TRANSACTIONS) {
                throw new AplException.NotValidException("Invalid number of linkedFullHashes " + linkedFullHashes.length);
            }
            Set<Long> linkedTransactionIds = new HashSet<>(linkedFullHashes.length);
            for (byte[] hash : linkedFullHashes) {
                if (Convert.emptyToNull(hash) == null || hash.length != 32) {
                    throw new AplException.NotValidException("Invalid linkedFullHash " + Convert.toHexString(hash));
                }
                if (!linkedTransactionIds.add(Convert.fullHashToId(hash))) {
                    throw new AplException.NotValidException("Duplicate linked transaction ids");
                }
                checkLinkedTransaction(hash, currentHeight, transaction.getHeight());
            }
            if (params.getQuorum() > linkedFullHashes.length) {
                throw new AplException.NotValidException("Quorum of " + params.getQuorum() + " cannot be achieved in by-transaction voting with "
                        + linkedFullHashes.length + " linked full hashes only");
            }
        } else {
            if (linkedFullHashes.length != 0) {
                throw new AplException.NotValidException("LinkedFullHashes can only be used with VotingModel.TRANSACTION");
            }
        }

        if (params.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
            if (params.getQuorum() != 1) {
                throw new AplException.NotValidException("Quorum must be 1 for by-hash voting");
            }
            if (hashedSecret.length == 0 || hashedSecret.length > Byte.MAX_VALUE) {
                throw new AplException.NotValidException("Invalid hashedSecret " + Convert.toHexString(hashedSecret));
            }
            if (PhasingPollService.getHashFunction(algorithm) == null) {
                throw new AplException.NotValidException("Invalid hashedSecretAlgorithm " + algorithm);
            }
        } else {
            if (hashedSecret.length != 0) {
                throw new AplException.NotValidException("HashedSecret can only be used with VotingModel.HASH");
            }
            if (algorithm != 0) {
                throw new AplException.NotValidException("HashedSecretAlgorithm can only be used with VotingModel.HASH");
            }
        }
    }

    public void validateFinishHeight(Integer finishHeight) throws AplException.NotCurrentlyValidException{
        lookupBlockchain();
        Block lastBlock = blockchain.getLastBlock();
        int currentHeight = lastBlock.getHeight();

        if (this.finishHeight <= currentHeight + (params.getVoteWeighting().acceptsVotes() ? 2 : 1)
                || this.finishHeight >= currentHeight + Constants.MAX_PHASING_DURATION) {
            throw new AplException.NotCurrentlyValidException("Invalid finish height " + finishHeight);
        }

    }

    public void validateFinishHeightAndTime(Integer height, Integer time) throws AplException.NotCurrentlyValidException {

        if((this.finishHeight != -1 && time != -1) || (this.finishHeight == -1 && time == -1)){
            throw new AplException.NotCurrentlyValidException("Only one parameter should be filled 'phasingFinishHeight or phasingFinishTime'");
        }

        lookupBlockchain();
        TimeService timeService = CDI.current().select(TimeService.class).get();
        Block lastBlock = blockchain.getLastBlock();
        int lastBlockHeight = lastBlock.getHeight();
        int currentTime = timeService.getEpochTime();

        if (time == -1 &&
                (this.finishHeight <= lastBlockHeight + (params.getVoteWeighting().acceptsVotes() ? 2 : 1) ||
                        this.finishHeight >= lastBlockHeight + Constants.MAX_PHASING_DURATION)) {
            throw new AplException.NotCurrentlyValidException("Invalid finish height " + height);
        }


        if (this.finishHeight == -1 && time >= currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC){
                throw new AplException.NotCurrentlyValidException("Invalid finish time " + time);
        }

    }

    private void checkLinkedTransaction(byte[] hash, int currentHeight, int transactionHeight) throws AplException.NotValidException, AplException.NotCurrentlyValidException {
        Integer txHeight = lookupBlockchain().getTransactionHeight(hash, currentHeight);
        if (txHeight != null) {

            if (transactionHeight - txHeight > blockchainConfig.getCurrentConfig().getReferencedTransactionHeightSpan()) {
                throw new AplException.NotValidException("Linked transaction cannot be more than 60 days older than the phased transaction");
            }
            if (phasingPollService.isTransactionPhased(Convert.fullHashToId(hash))) {
                throw new AplException.NotCurrentlyValidException("Cannot link to an already existing phased transaction");
            }
        }
    }

    @Override
    public void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        params.checkApprovable();
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        phasingPollService.addPoll(transaction, this);
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return PHASING_FEE;
    }

    public int getFinishHeight() {
        return finishHeight;
    }

    public long getQuorum() {
        return params.getQuorum();
    }

    public long[] getWhitelist() {
        return params.getWhitelist();
    }

    public VoteWeighting getVoteWeighting() {
        return params.getVoteWeighting();
    }

    public byte[][] getLinkedFullHashes() {
        return linkedFullHashes;
    }

    public byte[] getHashedSecret() {
        return hashedSecret;
    }

    public byte getAlgorithm() {
        return algorithm;
    }

    public PhasingParams getParams() {
        return params;
    }

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

}