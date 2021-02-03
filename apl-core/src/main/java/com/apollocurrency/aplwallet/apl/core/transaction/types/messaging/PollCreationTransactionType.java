/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
public class PollCreationTransactionType extends MessagingTransactionType {
    private final PollService pollService;
    private final TransactionValidator transactionValidator;
    @Inject
    public PollCreationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, PollService pollService, TransactionValidator transactionValidator) {
        super(blockchainConfig, accountService);
        this.pollService = pollService;
        this.transactionValidator = transactionValidator;
    }

    private final Fee POLL_OPTIONS_FEE = new Fee.SizeBasedFee(Math.multiplyExact(10, getBlockchainConfig().getOneAPL()), getBlockchainConfig().getOneAPL(), 1) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            int numOptions = ((MessagingPollCreation) appendage).getPollOptions().length;
            return numOptions <= 19 ? 0 : numOptions - 19;
        }
    };
    private final Fee POLL_SIZE_FEE = new Fee.SizeBasedFee(0, Math.multiplyExact(2, getBlockchainConfig().getOneAPL()), 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            MessagingPollCreation attachment = (MessagingPollCreation) appendage;
            int size = attachment.getPollName().length() + attachment.getPollDescription().length();
            for (String option : ((MessagingPollCreation) appendage).getPollOptions()) {
                size += option.length();
            }
            return size <= 288 ? 0 : size - 288;
        }
    };
    private final Fee POLL_FEE = (transaction, appendage) -> POLL_OPTIONS_FEE.getFee(transaction, appendage) + POLL_SIZE_FEE.getFee(transaction, appendage);


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.POLL_CREATION;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.POLL_CREATION;
    }

    @Override
    public String getName() {
        return "PollCreation";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return POLL_FEE;
    }

    @Override
    public MessagingPollCreation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingPollCreation(buffer);
    }

    @Override
    public MessagingPollCreation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingPollCreation(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingPollCreation attachment = (MessagingPollCreation) transaction.getAttachment();
        pollService.addPoll(transaction, attachment);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingPollCreation attachment = (MessagingPollCreation) transaction.getAttachment();
        attachment.getVoteWeighting().validateStateDependent();
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingPollCreation attachment = (MessagingPollCreation) transaction.getAttachment();
        int optionsCount = attachment.getPollOptions().length;
        if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH || attachment.getPollName().isEmpty() || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH || optionsCount > Constants.MAX_POLL_OPTION_COUNT || optionsCount == 0) {
            throw new AplException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
        }
        if (attachment.getMinNumberOfOptions() < 1 || attachment.getMinNumberOfOptions() > optionsCount) {
            throw new AplException.NotValidException("Invalid min number of options: " + attachment.getJSONObject());
        }
        if (attachment.getMaxNumberOfOptions() < 1 || attachment.getMaxNumberOfOptions() < attachment.getMinNumberOfOptions() || attachment.getMaxNumberOfOptions() > optionsCount) {
            throw new AplException.NotValidException("Invalid max number of options: " + attachment.getJSONObject());
        }
        for (int i = 0; i < optionsCount; i++) {
            if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH || attachment.getPollOptions()[i].isEmpty()) {
                throw new AplException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
            }
        }
        if (attachment.getMinRangeValue() < Constants.MIN_VOTE_VALUE || attachment.getMaxRangeValue() > Constants.MAX_VOTE_VALUE || attachment.getMaxRangeValue() < attachment.getMinRangeValue()) {
            throw new AplException.NotValidException("Invalid range: " + attachment.getJSONObject());
        }
        int finishValidationHeight = transactionValidator.getFinishValidationHeight(transaction, attachment);
        if (attachment.getFinishHeight() <= finishValidationHeight + 1 || attachment.getFinishHeight() >= finishValidationHeight + Constants.MAX_POLL_DURATION) {
            throw new AplException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
        }
        if (!attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
            throw new AplException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
        }
        attachment.getVoteWeighting().validateStateIndependent();
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), getName(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
