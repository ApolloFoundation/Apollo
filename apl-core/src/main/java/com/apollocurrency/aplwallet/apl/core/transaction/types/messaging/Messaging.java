/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountProperty;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountPropertyDelete;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasBuy;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasDelete;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author al
 */
public abstract class Messaging extends TransactionType {
    public static final TransactionType VOTE_CASTING = new VoteCastingTransactionType();
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
            lookupAccountInfoService().updateAccountInfo(senderAccount, attachment.getName(), attachment.getDescription());
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
                throw new AplException.NotValidException("Account property transaction cannot be used to send " + lookupBlockchainConfig().getCoinSymbol());
            }
            if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
                throw new AplException.NotValidException("Setting Genesis account properties not allowed");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
            lookupAccountPropertyService().setProperty(recipientAccount, transaction, senderAccount, attachment.getProperty(), attachment.getValue());
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
            AccountProperty accountProperty = lookupAccountPropertyService().getProperty(attachment.getPropertyId());
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
                throw new AplException.NotValidException("Account property transaction cannot be used to send " + lookupBlockchainConfig().getCoinSymbol());
            }
            if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
                throw new AplException.NotValidException("Deleting Genesis account properties not allowed");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            MessagingAccountPropertyDelete attachment = (MessagingAccountPropertyDelete) transaction.getAttachment();
            lookupAccountPropertyService().deleteProperty(senderAccount, attachment.getPropertyId());
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

    private static PhasingPollService phasingPollService;// lazy init
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
                PhasingPollService phasingPollService = lookupPhasingPollService();
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
                if (poll.getFinishTime() == -1 && poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1) {
                    throw new AplException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d", poll.getFinishHeight(), attachment.getFinishValidationHeight(transaction) + 1));
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
                lookupPhasingPollService().addVote(transaction, senderAccount, Convert.fullHashToId(hash));
            }
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }
    };
    private static volatile AliasService ALIAS_SERVICE;
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
            lookupAliasService().addOrUpdateAlias(transaction, attachment);
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
            return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
        }

        @Override
        public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return lookupAliasService().getAliasByName(((MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null && isDuplicate(Messaging.ALIAS_ASSIGNMENT, "", duplicates, true);
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
            Alias alias = lookupAliasService().getAliasByName(normalizedAlias);
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
            lookupAliasService().sellAlias(transaction, attachment);
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
            if (priceATM < 0 || priceATM > lookupBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid alias sell price: " + priceATM);
            }
            if (priceATM == 0) {
                if (GenesisImporter.CREATOR_ID == transaction.getRecipientId()) {
                    throw new AplException.NotValidException("Transferring aliases to Genesis account not allowed");
                } else if (transaction.getRecipientId() == 0) {
                    throw new AplException.NotValidException("Missing alias transfer recipient");
                }
            }
            final Alias alias = lookupAliasService().getAliasByName(aliasName);
            if (alias == null) {
                throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
            } else if (alias.getAccountId() != transaction.getSenderId()) {
                throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
            }
            if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
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
            lookupAliasService().changeOwner(transaction.getSenderId(), aliasName);
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
            final Alias alias = lookupAliasService().getAliasByName(aliasName);
            if (alias == null) {
                throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
            } else if (alias.getAccountId() != transaction.getRecipientId()) {
                throw new AplException.NotCurrentlyValidException("Alias is owned by account other than recipient: " + Long.toUnsignedString(alias.getAccountId()));
            }
            AliasOffer offer = lookupAliasService().getOffer(alias);
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
            lookupAliasService().deleteAlias(attachment.getAliasName());
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
            final Alias alias = lookupAliasService().getAliasByName(aliasName);
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

    public Messaging(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
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

}
