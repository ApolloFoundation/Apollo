/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
public class VoteCastingTransactionType extends Messaging {
    private final PollService pollService;

    @Inject
    public VoteCastingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, PollService pollService) {
        super(blockchainConfig, accountService);
        this.pollService = pollService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.VOTE_CASTING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.VOTE_CASTING;
    }

    @Override
    public String getName() {
        return "VoteCasting";
    }

    @Override
    public MessagingVoteCasting parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingVoteCasting(buffer);
    }

    @Override
    public MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingVoteCasting(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingVoteCasting attachment = (MessagingVoteCasting) transaction.getAttachment();
        Vote.addVote(transaction, attachment);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MessagingVoteCasting attachment = (MessagingVoteCasting) transaction.getAttachment();
        if (attachment.getPollId() == 0 || attachment.getPollVote() == null || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
            throw new AplException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
        }
        long pollId = attachment.getPollId();
        Poll poll = pollService.getPoll(pollId);
        if (poll == null) {
            throw new AplException.NotCurrentlyValidException("Invalid poll: " + Long.toUnsignedString(attachment.getPollId()));
        }
        if (Vote.getVote(pollId, transaction.getSenderId()) != null) {
            throw new AplException.NotCurrentlyValidException("Double voting attempt");
        }
        if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction)) {
            throw new AplException.NotCurrentlyValidException("Voting for this poll finishes at " + poll.getFinishHeight());
        }
        byte[] votes = attachment.getPollVote();
        int positiveCount = 0;
        for (byte vote : votes) {
            if (vote != Constants.NO_VOTE_VALUE && (vote < poll.getMinRangeValue() || vote > poll.getMaxRangeValue())) {
                throw new AplException.NotValidException(String.format("Invalid vote %d, vote must be between %d and %d", vote, poll.getMinRangeValue(), poll.getMaxRangeValue()));
            }
            if (vote != Constants.NO_VOTE_VALUE) {
                positiveCount++;
            }
        }
        if (positiveCount < poll.getMinNumberOfOptions() || positiveCount > poll.getMaxNumberOfOptions()) {
            throw new AplException.NotValidException(String.format("Invalid num of choices %d, number of choices must be between %d and %d", positiveCount, poll.getMinNumberOfOptions(), poll.getMaxNumberOfOptions()));
        }
    }

    @Override
    public boolean isDuplicate(final Transaction transaction, final Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MessagingVoteCasting attachment = (MessagingVoteCasting) transaction.getAttachment();
        String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
        return isDuplicate(TransactionTypes.TransactionTypeSpec.VOTE_CASTING, key, duplicates, true);
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
