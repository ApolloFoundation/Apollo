/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountProperty;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountPropertyDelete;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasBuy;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasDelete;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
public abstract class Messaging extends TransactionType {
    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    private Messaging() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_MESSAGING;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }
    public static final TransactionType ARBITRARY_MESSAGE = new Messaging() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ARBITRARY_MESSAGE;
        }

        @Override
        public String getName() {
            return "ArbitraryMessage";
        }

        @Override
        public EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return Attachment.ARBITRARY_MESSAGE;
        }

        @Override
        public EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return Attachment.ARBITRARY_MESSAGE;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            Attachment attachment = transaction.getAttachment();
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new AplException.NotValidException("Sending messages to Genesis not allowed.");
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean mustHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    
    public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {
        private final Fee ALIAS_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
                return attachment.getAliasName().length() + attachment.getAliasURI().length();
            }
        };

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ALIAS_ASSIGNMENT;
        }

        @Override
        public String getName() {
            return "AliasAssignment";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return ALIAS_FEE;
        }

        @Override
        public MessagingAliasAssignment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAliasAssignment(buffer);
        }

        @Override
        public MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAliasAssignment(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
            Alias.addOrUpdateAlias(transaction, attachment);
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
            return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
        }

        @Override
        public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return Alias.getAlias(((MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null && isDuplicate(Messaging.ALIAS_ASSIGNMENT, "", duplicates, true);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
            if (attachment.getAliasName().length() == 0 || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                throw new AplException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
            }
            String normalizedAlias = attachment.getAliasName().toLowerCase();
            for (int i = 0; i < normalizedAlias.length(); i++) {
                if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                    throw new AplException.NotValidException("Invalid alias name: " + normalizedAlias);
                }
            }
            Alias alias = Alias.getAlias(normalizedAlias);
            if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType ALIAS_SELL = new Messaging() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ALIAS_SELL;
        }

        @Override
        public String getName() {
            return "AliasSell";
        }

        @Override
        public MessagingAliasSell parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAliasSell(buffer);
        }

        @Override
        public MessagingAliasSell parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAliasSell(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
            Alias.sellAlias(transaction, attachment);
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
            // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
            return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Invalid sell alias transaction: " + transaction.getJSONObject());
            }
            final MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
            final String aliasName = attachment.getAliasName();
            if (aliasName == null || aliasName.length() == 0) {
                throw new AplException.NotValidException("Missing alias name");
            }
            long priceATM = attachment.getPriceATM();
            if (priceATM < 0 || priceATM > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid alias sell price: " + priceATM);
            }
            if (priceATM == 0) {
                if (Genesis.CREATOR_ID == transaction.getRecipientId()) {
                    throw new AplException.NotValidException("Transferring aliases to Genesis account not allowed");
                } else if (transaction.getRecipientId() == 0) {
                    throw new AplException.NotValidException("Missing alias transfer recipient");
                }
            }
            final Alias alias = Alias.getAlias(aliasName);
            if (alias == null) {
                throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
            } else if (alias.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new AplException.NotValidException("Selling alias to Genesis not allowed");
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean mustHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType ALIAS_BUY = new Messaging() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ALIAS_BUY;
        }

        @Override
        public String getName() {
            return "AliasBuy";
        }

        @Override
        public MessagingAliasBuy parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAliasBuy(buffer);
        }

        @Override
        public MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAliasBuy(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            final MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
            final String aliasName = attachment.getAliasName();
            Alias.changeOwner(transaction.getSenderId(), aliasName);
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
            // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
            return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            final MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
            final String aliasName = attachment.getAliasName();
            final Alias alias = Alias.getAlias(aliasName);
            if (alias == null) {
                throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
            } else if (alias.getAccountId() != transaction.getRecipientId()) {
                throw new AplException.NotCurrentlyValidException("Alias is owned by account other than recipient: " + Long.toUnsignedString(alias.getAccountId()));
            }
            Alias.Offer offer = Alias.getOffer(alias);
            if (offer == null) {
                throw new AplException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
            }
            if (transaction.getAmountATM() < offer.getPriceATM()) {
                String msg = "Price is too low for: " + aliasName + " (" + transaction.getAmountATM() + " < " + offer.getPriceATM() + ")";
                throw new AplException.NotCurrentlyValidException(msg);
            }
            if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
                throw new AplException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": " + Long.toUnsignedString(transaction.getSenderId()) + " expected: " + Long.toUnsignedString(offer.getBuyerId()));
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType ALIAS_DELETE = new Messaging() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ALIAS_DELETE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ALIAS_DELETE;
        }

        @Override
        public String getName() {
            return "AliasDelete";
        }

        @Override
        public MessagingAliasDelete parseAttachment(final ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAliasDelete(buffer);
        }

        @Override
        public MessagingAliasDelete parseAttachment(final JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAliasDelete(attachmentData);
        }

        @Override
        public void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
            final MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
            Alias.deleteAlias(attachment.getAliasName());
        }

        @Override
        public boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
            // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
            return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
        }

        @Override
        public void validateAttachment(final Transaction transaction) throws AplException.ValidationException {
            final MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
            final String aliasName = attachment.getAliasName();
            if (aliasName == null || aliasName.length() == 0) {
                throw new AplException.NotValidException("Missing alias name");
            }
            final Alias alias = Alias.getAlias(aliasName);
            if (alias == null) {
                throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
            } else if (alias.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType POLL_CREATION = new Messaging() {
        private final Fee POLL_OPTIONS_FEE = new Fee.SizeBasedFee(10 * Constants.ONE_APL, Constants.ONE_APL, 1) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                int numOptions = ((MessagingPollCreation) appendage).getPollOptions().length;
                return numOptions <= 19 ? 0 : numOptions - 19;
            }
        };
        private final Fee POLL_SIZE_FEE = new Fee.SizeBasedFee(0, 2 * Constants.ONE_APL, 32) {
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
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
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
            Poll.addPoll(transaction, attachment);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
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
            if (attachment.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1 || attachment.getFinishHeight() >= attachment.getFinishValidationHeight(transaction) + Constants.MAX_POLL_DURATION) {
                throw new AplException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
            }
            if (!attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                throw new AplException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
            }
            attachment.getVoteWeighting().validate();
        }

        @Override
        public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return isDuplicate(Messaging.POLL_CREATION, getName(), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType VOTE_CASTING = new Messaging() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
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
            Poll poll = Poll.getPoll(pollId);
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
        public boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingVoteCasting attachment = (MessagingVoteCasting) transaction.getAttachment();
            String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
            return isDuplicate(Messaging.VOTE_CASTING, key, duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };
    public static final TransactionType PHASING_VOTE_CASTING = new Messaging() {
        private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
            MessagingPhasingVoteCasting attachment = (MessagingPhasingVoteCasting) transaction.getAttachment();
            return attachment.getTransactionFullHashes().size() * Constants.ONE_APL;
        };

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
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
                    if (hashedSecret == null && !poll.verifySecret(revealedSecret)) {
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
                if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1) {
                    throw new AplException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d", poll.getFinishHeight(), attachment.getFinishValidationHeight(transaction) + 1));
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
    };
    public static final Messaging ACCOUNT_INFO = new Messaging() {
        private final Fee ACCOUNT_INFO_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, 2 * Constants.ONE_APL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
                return attachment.getName().length() + attachment.getDescription().length();
            }
        };

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ACCOUNT_INFO;
        }

        @Override
        public String getName() {
            return "AccountInfo";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return ACCOUNT_INFO_FEE;
        }

        @Override
        public MessagingAccountInfo parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAccountInfo(buffer);
        }

        @Override
        public MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAccountInfo(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
            if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
                throw new AplException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
            senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
        }

        @Override
        public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return isDuplicate(Messaging.ACCOUNT_INFO, getName(), duplicates, true);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }
    };
    public static final Messaging ACCOUNT_PROPERTY = new Messaging() {
        private final Fee ACCOUNT_PROPERTY_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
                return attachment.getValue().length();
            }
        };

        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ACCOUNT_PROPERTY;
        }

        @Override
        public String getName() {
            return "AccountProperty";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return ACCOUNT_PROPERTY_FEE;
        }

        @Override
        public MessagingAccountProperty parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAccountProperty(buffer);
        }

        @Override
        public MessagingAccountProperty parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAccountProperty(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
            if (attachment.getProperty().length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH || attachment.getProperty().length() == 0 || attachment.getValue().length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
                throw new AplException.NotValidException("Invalid account property: " + attachment.getJSONObject());
            }
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Account property transaction cannot be used to send " + blockchainConfig.getCoinSymbol());
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new AplException.NotValidException("Setting Genesis account properties not allowed");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
            recipientAccount.setProperty(transaction, senderAccount, attachment.getProperty(), attachment.getValue());
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }
    };
    public static final Messaging ACCOUNT_PROPERTY_DELETE = new Messaging() {
        @Override
        public byte getSubtype() {
            return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ACCOUNT_PROPERTY_DELETE;
        }

        @Override
        public String getName() {
            return "AccountPropertyDelete";
        }

        @Override
        public MessagingAccountPropertyDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MessagingAccountPropertyDelete(buffer);
        }

        @Override
        public MessagingAccountPropertyDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new MessagingAccountPropertyDelete(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            MessagingAccountPropertyDelete attachment = (MessagingAccountPropertyDelete) transaction.getAttachment();
            AccountProperty accountProperty = AccountPropertyTable.getProperty(attachment.getPropertyId());
            if (accountProperty == null) {
                throw new AplException.NotCurrentlyValidException("No such property " + Long.toUnsignedString(attachment.getPropertyId()));
            }
            if (accountProperty.getRecipientId() != transaction.getSenderId() && accountProperty.getSetterId() != transaction.getSenderId()) {
                throw new AplException.NotValidException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " cannot delete property " + Long.toUnsignedString(attachment.getPropertyId()));
            }
            if (accountProperty.getRecipientId() != transaction.getRecipientId()) {
                throw new AplException.NotValidException("Account property " + Long.toUnsignedString(attachment.getPropertyId()) + " does not belong to " + Long.toUnsignedString(transaction.getRecipientId()));
            }
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Account property transaction cannot be used to send " + blockchainConfig.getCoinSymbol());
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new AplException.NotValidException("Deleting Genesis account properties not allowed");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAccountPropertyDelete attachment = (MessagingAccountPropertyDelete) transaction.getAttachment();
            senderAccount.deleteProperty(attachment.getPropertyId());
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }
    };
    
}
