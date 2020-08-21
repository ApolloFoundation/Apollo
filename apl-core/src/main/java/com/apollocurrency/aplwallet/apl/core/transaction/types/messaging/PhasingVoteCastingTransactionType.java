/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
@Singleton
public class PhasingVoteCastingTransactionType extends MessagingTransactionType {
    private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
        MessagingPhasingVoteCasting attachment = (MessagingPhasingVoteCasting) transaction.getAttachment();
        return attachment.getTransactionFullHashes().size() * Constants.ONE_APL;
    };

    private final PhasingPollService phasingPollService;
    private final TransactionValidator transactionValidator;

    @Inject
    public PhasingVoteCastingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, PhasingPollService phasingPollService, TransactionValidator transactionValidator) {
        super(blockchainConfig, accountService);
        this.phasingPollService = phasingPollService;
        this.transactionValidator = transactionValidator;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.PHASING_VOTE_CASTING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.PHASING_VOTE_CASTING;
    }

    @Override
    public String getName() {
        return "PhasingVoteCasting";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return PHASING_VOTE_FEE;
    }

    @Override
    public MessagingPhasingVoteCasting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingPhasingVoteCasting(buffer);
    }

    @Override
    public MessagingPhasingVoteCasting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingPhasingVoteCasting(attachmentData);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MessagingPhasingVoteCasting attachment = (MessagingPhasingVoteCasting) transaction.getAttachment();
        byte[] revealedSecret = attachment.getRevealedSecret();
        if (revealedSecret.length > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
            throw new AplException.NotValidException("Invalid revealed secret length " + revealedSecret.length);
        }
        byte[] hashedSecret = null;
        byte algorithm = 0;
        List<byte[]> hashes = attachment.getTransactionFullHashes();
        if (hashes.size() > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
            throw new AplException.NotValidException("No more than " + Constants.MAX_PHASING_VOTE_TRANSACTIONS + " votes allowed for two-phased multi-voting");
        }
        long voterId = transaction.getSenderId();
        for (byte[] hash : hashes) {
            long phasedTransactionId = Convert.fullHashToId(hash);
            if (phasedTransactionId == 0) {
                throw new AplException.NotValidException("Invalid phased transactionFullHash " + Convert.toHexString(hash));
            };
            PhasingPollResult result = phasingPollService.getResult(phasedTransactionId);
            if (result != null) {
                throw new AplException.NotCurrentlyValidException("Phasing poll " + phasedTransactionId + " is already finished");
            }
            PhasingPoll poll = phasingPollService.getPoll(phasedTransactionId);
            if (poll == null) {
                throw new AplException.NotCurrentlyValidException("Invalid phased transaction " + Long.toUnsignedString(phasedTransactionId) + ", or phasing is finished");
            }
            if (!poll.getVoteWeighting().acceptsVotes()) {
                throw new AplException.NotValidException("This phased transaction does not require or accept voting");
            }
            long[] whitelist = poll.getWhitelist();
            if (whitelist.length > 0 && Arrays.binarySearch(whitelist, voterId) < 0) {
                throw new AplException.NotValidException("Voter is not in the phased transaction whitelist");
            }
            if (revealedSecret.length > 0) {
                if (poll.getVoteWeighting().getVotingModel() != VoteWeighting.VotingModel.HASH) {
                    throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " does not accept by-hash voting");
                }
                if (hashedSecret != null && !Arrays.equals(poll.getHashedSecret(), hashedSecret)) {
                    throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecret");
                }
                if (algorithm != 0 && algorithm != poll.getAlgorithm()) {
                    throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecretAlgorithm");
                }
                if (hashedSecret == null && !phasingPollService.verifySecret(poll, revealedSecret)) {
                    throw new AplException.NotValidException("Revealed secret does not match phased transaction hashed secret");
                }
                hashedSecret = poll.getHashedSecret();
                algorithm = poll.getAlgorithm();
            } else if (poll.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                throw new AplException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " requires revealed secret for approval");
            }
            if (!Arrays.equals(poll.getFullHash(), hash)) {
                throw new AplException.NotCurrentlyValidException("Phased transaction hash does not match hash in voting transaction");
            }
            int finishHeight = transactionValidator.getFinishValidationHeight(transaction, attachment) + 1;
            if (poll.getFinishTime() == -1 && poll.getFinishHeight() <= finishHeight) {
                throw new AplException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d", poll.getFinishHeight(), finishHeight));
            }

            if (poll.getFinishHeight() == -1 && poll.getFinishTime() <= transaction.getTimestamp()) {
                throw new AplException.NotCurrentlyValidException(String.format("Phased transaction finishes at timestamp %d which is not after approval transaction timestamp %d", poll.getFinishTime(), transaction.getTimestamp()));
            }

        }
    }

    @Override
    public final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingPhasingVoteCasting attachment = (MessagingPhasingVoteCasting) transaction.getAttachment();
        List<byte[]> hashes = attachment.getTransactionFullHashes();
        for (byte[] hash : hashes) {
            phasingPollService.addVote(transaction, senderAccount, Convert.fullHashToId(hash));
        }
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
}
