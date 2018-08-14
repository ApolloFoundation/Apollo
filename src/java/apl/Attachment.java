/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package apl;

import apl.crypto.Crypto;
import apl.crypto.EncryptedData;
import apl.updater.Architecture;
import apl.updater.Platform;
import apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Attachment extends Appendix {

    TransactionType getTransactionType();

    abstract class AbstractAttachment extends Appendix.AbstractAppendix implements Attachment {

        private AbstractAttachment(ByteBuffer buffer) {
            super(buffer);
        }

        private AbstractAttachment(JSONObject attachmentData) {
            super(attachmentData);
        }

        private AbstractAttachment(int version) {
            super(version);
        }

        private AbstractAttachment() {
        }

        @Override
        final String getAppendixName() {
            return getTransactionType().getName();
        }

        @Override
        final void validate(Transaction transaction) throws AplException.ValidationException {
            getTransactionType().validateAttachment(transaction);
        }

        @Override
        final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            getTransactionType().apply((TransactionImpl) transaction, senderAccount, recipientAccount);
        }

        @Override
        public final Fee getBaselineFee(Transaction transaction) {
            return getTransactionType().getBaselineFee(transaction);
        }

        @Override
        public final Fee getNextFee(Transaction transaction) {
            return getTransactionType().getNextFee(transaction);
        }

        @Override
        public final int getBaselineFeeHeight() {
            return getTransactionType().getBaselineFeeHeight();
        }

        @Override
        public final int getNextFeeHeight() {
            return getTransactionType().getNextFeeHeight();
        }

        @Override
        final boolean isPhasable() {
            return !(this instanceof Prunable) && getTransactionType().isPhasable();
        }

        final int getFinishValidationHeight(Transaction transaction) {
            return isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : Apl.getBlockchain().getHeight();
        }

    }

    abstract class EmptyAttachment extends AbstractAttachment {

        private EmptyAttachment() {
            super(0);
        }

        @Override
        final int getMySize() {
            return 0;
        }

        @Override
        final void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        final void putMyJSON(JSONObject json) {
        }

        @Override
        final boolean verifyVersion() {
            return getVersion() == 0;
        }

    }

    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.ORDINARY;
        }

    };

    EmptyAttachment PRIVATE_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.PRIVATE;
        }

    };


    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

    };

    final class MessagingAliasAssignment extends AbstractAttachment {

        private final String aliasName;
        private final String aliasURI;

        MessagingAliasAssignment(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        }

        MessagingAliasAssignment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = Convert.nullToEmpty((String) attachmentData.get("alias")).trim();
            aliasURI = Convert.nullToEmpty((String) attachmentData.get("uri")).trim();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI) {
            this.aliasName = aliasName.trim();
            this.aliasURI = aliasURI.trim();
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte) alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    final class MessagingAliasSell extends AbstractAttachment {

        private final String aliasName;
        private final long priceATM;

        MessagingAliasSell(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
            this.priceATM = buffer.getLong();
        }

        MessagingAliasSell(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
            this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public MessagingAliasSell(String aliasName, long priceATM) {
            this.aliasName = aliasName;
            this.priceATM = priceATM;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("priceNQT", priceATM);
        }

        public String getAliasName() {
            return aliasName;
        }

        public long getPriceATM() {
            return priceATM;
        }
    }

    final class MessagingAliasBuy extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasBuy(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasBuy(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasBuy(String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName() {
            return aliasName;
        }
    }

    final class MessagingAliasDelete extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasDelete(final ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasDelete(final JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasDelete(final String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_DELETE;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        void putMyBytes(final ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        void putMyJSON(final JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName() {
            return aliasName;
        }
    }

    final class MessagingPollCreation extends AbstractAttachment {

        public final static class PollBuilder {
            private final String pollName;
            private final String pollDescription;
            private final String[] pollOptions;

            private final int finishHeight;
            private final byte votingModel;

            private long minBalance = 0;
            private byte minBalanceModel;

            private final byte minNumberOfOptions;
            private final byte maxNumberOfOptions;

            private final byte minRangeValue;
            private final byte maxRangeValue;

            private long holdingId;

            public PollBuilder(final String pollName, final String pollDescription, final String[] pollOptions,
                               final int finishHeight, final byte votingModel,
                               byte minNumberOfOptions, byte maxNumberOfOptions,
                               byte minRangeValue, byte maxRangeValue) {
                this.pollName = pollName;
                this.pollDescription = pollDescription;
                this.pollOptions = pollOptions;

                this.finishHeight = finishHeight;
                this.votingModel = votingModel;
                this.minNumberOfOptions = minNumberOfOptions;
                this.maxNumberOfOptions = maxNumberOfOptions;
                this.minRangeValue = minRangeValue;
                this.maxRangeValue = maxRangeValue;

                this.minBalanceModel = VoteWeighting.VotingModel.get(votingModel).getMinBalanceModel().getCode();
            }

            public PollBuilder minBalance(byte minBalanceModel, long minBalance) {
                this.minBalanceModel = minBalanceModel;
                this.minBalance = minBalance;
                return this;
            }

            public PollBuilder holdingId(long holdingId) {
                this.holdingId = holdingId;
                return this;
            }

            public MessagingPollCreation build() {
                return new MessagingPollCreation(this);
            }
        }

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;

        private final int finishHeight;

        private final byte minNumberOfOptions;
        private final byte maxNumberOfOptions;
        private final byte minRangeValue;
        private final byte maxRangeValue;
        private final VoteWeighting voteWeighting;

        MessagingPollCreation(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.pollName = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
            this.pollDescription = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);

            this.finishHeight = buffer.getInt();

            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new AplException.NotValidException("Invalid number of poll options: " + numberOfOptions);
            }

            this.pollOptions = new String[numberOfOptions];
            for (int i = 0; i < numberOfOptions; i++) {
                this.pollOptions[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
            }

            byte votingModel = buffer.get();

            this.minNumberOfOptions = buffer.get();
            this.maxNumberOfOptions = buffer.get();

            this.minRangeValue = buffer.get();
            this.maxRangeValue = buffer.get();

            long minBalance = buffer.getLong();
            byte minBalanceModel = buffer.get();
            long holdingId = buffer.getLong();
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        MessagingPollCreation(JSONObject attachmentData) {
            super(attachmentData);

            this.pollName = ((String) attachmentData.get("name")).trim();
            this.pollDescription = ((String) attachmentData.get("description")).trim();
            this.finishHeight = ((Long) attachmentData.get("finishHeight")).intValue();

            JSONArray options = (JSONArray) attachmentData.get("options");
            this.pollOptions = new String[options.size()];
            for (int i = 0; i < pollOptions.length; i++) {
                this.pollOptions[i] = ((String) options.get(i)).trim();
            }
            byte votingModel = ((Long) attachmentData.get("votingModel")).byteValue();

            this.minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
            this.maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
            this.minRangeValue = ((Long) attachmentData.get("minRangeValue")).byteValue();
            this.maxRangeValue = ((Long) attachmentData.get("maxRangeValue")).byteValue();

            long minBalance = Convert.parseLong(attachmentData.get("minBalance"));
            byte minBalanceModel = ((Long) attachmentData.get("minBalanceModel")).byteValue();
            long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        private MessagingPollCreation(PollBuilder builder) {
            this.pollName = builder.pollName;
            this.pollDescription = builder.pollDescription;
            this.pollOptions = builder.pollOptions;
            this.finishHeight = builder.finishHeight;
            this.minNumberOfOptions = builder.minNumberOfOptions;
            this.maxNumberOfOptions = builder.maxNumberOfOptions;
            this.minRangeValue = builder.minRangeValue;
            this.maxRangeValue = builder.maxRangeValue;
            this.voteWeighting = new VoteWeighting(builder.votingModel, builder.holdingId, builder.minBalance, builder.minBalanceModel);
        }

        @Override
        int getMySize() {
            int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
            for (String pollOption : pollOptions) {
                size += 2 + Convert.toBytes(pollOption).length;
            }

            size += 4 + 1 + 1 + 1 + 1 + 1 + 8 + 1 + 8;

            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.pollName);
            byte[] description = Convert.toBytes(this.pollDescription);
            byte[][] options = new byte[this.pollOptions.length][];
            for (int i = 0; i < this.pollOptions.length; i++) {
                options[i] = Convert.toBytes(this.pollOptions[i]);
            }

            buffer.putShort((short) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putInt(finishHeight);
            buffer.put((byte) options.length);
            for (byte[] option : options) {
                buffer.putShort((short) option.length);
                buffer.put(option);
            }
            buffer.put(this.voteWeighting.getVotingModel().getCode());

            buffer.put(this.minNumberOfOptions);
            buffer.put(this.maxNumberOfOptions);
            buffer.put(this.minRangeValue);
            buffer.put(this.maxRangeValue);

            buffer.putLong(this.voteWeighting.getMinBalance());
            buffer.put(this.voteWeighting.getMinBalanceModel().getCode());
            buffer.putLong(this.voteWeighting.getHoldingId());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            attachment.put("finishHeight", this.finishHeight);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);


            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);

            attachment.put("minRangeValue", this.minRangeValue);
            attachment.put("maxRangeValue", this.maxRangeValue);

            attachment.put("votingModel", this.voteWeighting.getVotingModel().getCode());

            attachment.put("minBalance", this.voteWeighting.getMinBalance());
            attachment.put("minBalanceModel", this.voteWeighting.getMinBalanceModel().getCode());
            attachment.put("holding", Long.toUnsignedString(this.voteWeighting.getHoldingId()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() {
            return pollName;
        }

        public String getPollDescription() {
            return pollDescription;
        }

        public int getFinishHeight() {
            return finishHeight;
        }

        public String[] getPollOptions() {
            return pollOptions;
        }

        public byte getMinNumberOfOptions() {
            return minNumberOfOptions;
        }

        public byte getMaxNumberOfOptions() {
            return maxNumberOfOptions;
        }

        public byte getMinRangeValue() {
            return minRangeValue;
        }

        public byte getMaxRangeValue() {
            return maxRangeValue;
        }

        public VoteWeighting getVoteWeighting() {
            return voteWeighting;
        }

    }

    final class MessagingVoteCasting extends AbstractAttachment {

        private final long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            pollId = buffer.getLong();
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new AplException.NotValidException("More than " + Constants.MAX_POLL_OPTION_COUNT + " options in a vote");
            }
            pollVote = new byte[numberOfOptions];
            buffer.get(pollVote);
        }

        public MessagingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            pollId = Convert.parseUnsignedLong((String) attachmentData.get("poll"));
            JSONArray vote = (JSONArray) attachmentData.get("vote");
            pollVote = new byte[vote.size()];
            for (int i = 0; i < pollVote.length; i++) {
                pollVote[i] = ((Long) vote.get(i)).byteValue();
            }
        }

        public MessagingVoteCasting(long pollId, byte[] pollVote) {
            this.pollId = pollId;
            this.pollVote = pollVote;
        }

        @Override
        int getMySize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.pollId);
            buffer.put((byte) this.pollVote.length);
            buffer.put(this.pollVote);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("poll", Long.toUnsignedString(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public long getPollId() {
            return pollId;
        }

        public byte[] getPollVote() {
            return pollVote;
        }
    }

    final class MessagingPhasingVoteCasting extends AbstractAttachment {

        private final List<byte[]> transactionFullHashes;
        private final byte[] revealedSecret;

        MessagingPhasingVoteCasting(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            byte length = buffer.get();
            transactionFullHashes = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                byte[] hash = new byte[32];
                buffer.get(hash);
                transactionFullHashes.add(hash);
            }
            int secretLength = buffer.getInt();
            if (secretLength > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                throw new AplException.NotValidException("Invalid revealed secret length " + secretLength);
            }
            if (secretLength > 0) {
                revealedSecret = new byte[secretLength];
                buffer.get(revealedSecret);
            } else {
                revealedSecret = Convert.EMPTY_BYTE;
            }
        }

        MessagingPhasingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray hashes = (JSONArray) attachmentData.get("transactionFullHashes");
            transactionFullHashes = new ArrayList<>(hashes.size());
            hashes.forEach(hash -> transactionFullHashes.add(Convert.parseHexString((String) hash)));
            String revealedSecret = Convert.emptyToNull((String) attachmentData.get("revealedSecret"));
            this.revealedSecret = revealedSecret != null ? Convert.parseHexString(revealedSecret) : Convert.EMPTY_BYTE;
        }

        public MessagingPhasingVoteCasting(List<byte[]> transactionFullHashes, byte[] revealedSecret) {
            this.transactionFullHashes = transactionFullHashes;
            this.revealedSecret = revealedSecret;
        }

        @Override
        int getMySize() {
            return 1 + 32 * transactionFullHashes.size() + 4 + revealedSecret.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put((byte) transactionFullHashes.size());
            transactionFullHashes.forEach(buffer::put);
            buffer.putInt(revealedSecret.length);
            buffer.put(revealedSecret);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            JSONArray jsonArray = new JSONArray();
            transactionFullHashes.forEach(hash -> jsonArray.add(Convert.toHexString(hash)));
            attachment.put("transactionFullHashes", jsonArray);
            if (revealedSecret.length > 0) {
                attachment.put("revealedSecret", Convert.toHexString(revealedSecret));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.PHASING_VOTE_CASTING;
        }

        public List<byte[]> getTransactionFullHashes() {
            return transactionFullHashes;
        }

        public byte[] getRevealedSecret() {
            return revealedSecret;
        }
    }

    final class MessagingAccountInfo extends AbstractAttachment {

        private final String name;
        private final String description;

        MessagingAccountInfo(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        }

        MessagingAccountInfo(JSONObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        }

        public MessagingAccountInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    final class MessagingAccountProperty extends AbstractAttachment {

        private final String property;
        private final String value;

        MessagingAccountProperty(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.property = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH).trim();
            this.value = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH).trim();
        }

        MessagingAccountProperty(JSONObject attachmentData) {
            super(attachmentData);
            this.property = Convert.nullToEmpty((String) attachmentData.get("property")).trim();
            this.value = Convert.nullToEmpty((String) attachmentData.get("value")).trim();
        }

        public MessagingAccountProperty(String property, String value) {
            this.property = property.trim();
            this.value = Convert.nullToEmpty(value).trim();
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(property).length + 1 + Convert.toBytes(value).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] property = Convert.toBytes(this.property);
            byte[] value = Convert.toBytes(this.value);
            buffer.put((byte) property.length);
            buffer.put(property);
            buffer.put((byte) value.length);
            buffer.put(value);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("property", property);
            attachment.put("value", value);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_PROPERTY;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

    }

    final class MessagingAccountPropertyDelete extends AbstractAttachment {

        private final long propertyId;

        MessagingAccountPropertyDelete(ByteBuffer buffer) {
            super(buffer);
            this.propertyId = buffer.getLong();
        }

        MessagingAccountPropertyDelete(JSONObject attachmentData) {
            super(attachmentData);
            this.propertyId = Convert.parseUnsignedLong((String) attachmentData.get("property"));
        }

        public MessagingAccountPropertyDelete(long propertyId) {
            this.propertyId = propertyId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(propertyId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("property", Long.toUnsignedString(propertyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_PROPERTY_DELETE;
        }

        public long getPropertyId() {
            return propertyId;
        }

    }

    final class ColoredCoinsAssetIssuance extends AbstractAttachment {

        private final String name;
        private final String description;
        private final long quantityATU;
        private final byte decimals;

        ColoredCoinsAssetIssuance(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
            this.quantityATU = buffer.getLong();
            this.decimals = buffer.get();
        }

        ColoredCoinsAssetIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
            this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public ColoredCoinsAssetIssuance(String name, String description, long quantityATU, byte decimals) {
            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityATU = quantityATU;
            this.decimals = decimals;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putLong(quantityATU);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantityQNT", quantityATU);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getQuantityATU() {
            return quantityATU;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class ColoredCoinsAssetTransfer extends AbstractAttachment {

        private final long assetId;
        private final long quantityATU;

        ColoredCoinsAssetTransfer(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.assetId = buffer.getLong();
            this.quantityATU = buffer.getLong();
        }

        ColoredCoinsAssetTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
        }

        public ColoredCoinsAssetTransfer(long assetId, long quantityATU) {
            this.assetId = assetId;
            this.quantityATU = quantityATU;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityATU);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityATU);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityATU() {
            return quantityATU;
        }

    }

    final class ColoredCoinsAssetDelete extends AbstractAttachment {

        private final long assetId;
        private final long quantityATU;

        ColoredCoinsAssetDelete(ByteBuffer buffer) {
            super(buffer);
            this.assetId = buffer.getLong();
            this.quantityATU = buffer.getLong();
        }

        ColoredCoinsAssetDelete(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
            ;
        }

        public ColoredCoinsAssetDelete(long assetId, long quantityATU) {
            this.assetId = assetId;
            this.quantityATU = quantityATU;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityATU);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityATU);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_DELETE;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityATU() {
            return quantityATU;
        }

    }

    abstract class ColoredCoinsOrderPlacement extends AbstractAttachment {

        private final long assetId;
        private final long quantityATU;
        private final long priceATM;

        private ColoredCoinsOrderPlacement(ByteBuffer buffer) {
            super(buffer);
            this.assetId = buffer.getLong();
            this.quantityATU = buffer.getLong();
            this.priceATM = buffer.getLong();
        }

        private ColoredCoinsOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityATU = attachmentData.containsKey("quantityATU") ? Convert.parseLong(attachmentData.get("quantityATU")) : Convert.parseLong(attachmentData.get("quantityQNT"));
            this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
        }

        private ColoredCoinsOrderPlacement(long assetId, long quantityATU, long priceATM) {
            this.assetId = assetId;
            this.quantityATU = quantityATU;
            this.priceATM = priceATM;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityATU);
            buffer.putLong(priceATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityATU);
            attachment.put("priceNQT", priceATM);
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityATU() {
            return quantityATU;
        }

        public long getPriceATM() {
            return priceATM;
        }
    }

    final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsAskOrderPlacement(ByteBuffer buffer) {
            super(buffer);
        }

        ColoredCoinsAskOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderPlacement(long assetId, long quantityATU, long priceATM) {
            super(assetId, quantityATU, priceATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsBidOrderPlacement(ByteBuffer buffer) {
            super(buffer);
        }

        ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderPlacement(long assetId, long quantityATU, long priceATM) {
            super(assetId, quantityATU, priceATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract class ColoredCoinsOrderCancellation extends AbstractAttachment {

        private final long orderId;

        private ColoredCoinsOrderCancellation(ByteBuffer buffer) {
            super(buffer);
            this.orderId = buffer.getLong();
        }

        private ColoredCoinsOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
            this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
        }

        private ColoredCoinsOrderCancellation(long orderId) {
            this.orderId = orderId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(orderId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("order", Long.toUnsignedString(orderId));
        }

        public long getOrderId() {
            return orderId;
        }
    }

    final class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsAskOrderCancellation(ByteBuffer buffer) {
            super(buffer);
        }

        ColoredCoinsAskOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    final class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsBidOrderCancellation(ByteBuffer buffer) {
            super(buffer);
        }

        ColoredCoinsBidOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    final class ColoredCoinsDividendPayment extends AbstractAttachment {

        private final long assetId;
        private final int height;
        private final long amountATMPerATU;

        ColoredCoinsDividendPayment(ByteBuffer buffer) {
            super(buffer);
            this.assetId = buffer.getLong();
            this.height = buffer.getInt();
            this.amountATMPerATU = buffer.getLong();
        }

        ColoredCoinsDividendPayment(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.height = ((Long) attachmentData.get("height")).intValue();
            this.amountATMPerATU = attachmentData.containsKey("amountATMPerATU") ? Convert.parseLong(attachmentData.get("amountATMPerATU")) : Convert.parseLong(attachmentData.get("amountNQTPerQNT"));
        }

        public ColoredCoinsDividendPayment(long assetId, int height, long amountATMPerATU) {
            this.assetId = assetId;
            this.height = height;
            this.amountATMPerATU = amountATMPerATU;
        }

        @Override
        int getMySize() {
            return 8 + 4 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putInt(height);
            buffer.putLong(amountATMPerATU);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("height", height);
            attachment.put("amountNQTPerQNT", amountATMPerATU);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.DIVIDEND_PAYMENT;
        }

        public long getAssetId() {
            return assetId;
        }

        public int getHeight() {
            return height;
        }

        public long getAmountATMPerATU() {
            return amountATMPerATU;
        }

    }

    final class DigitalGoodsListing extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceATM;

        DigitalGoodsListing(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceATM = buffer.getLong();
        }

        DigitalGoodsListing(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = (String) attachmentData.get("description");
            this.tags = (String) attachmentData.get("tags");
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceATM) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceATM = priceATM;
        }

        @Override
        int getMySize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                    + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short) tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceATM() {
            return priceATM;
        }

    }

    final class DigitalGoodsDelisting extends AbstractAttachment {

        private final long goodsId;

        DigitalGoodsDelisting(ByteBuffer buffer) {
            super(buffer);
            this.goodsId = buffer.getLong();
        }

        DigitalGoodsDelisting(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        }

        public DigitalGoodsDelisting(long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public long getGoodsId() {
            return goodsId;
        }

    }

    final class DigitalGoodsPriceChange extends AbstractAttachment {

        private final long goodsId;
        private final long priceATM;

        DigitalGoodsPriceChange(ByteBuffer buffer) {
            super(buffer);
            this.goodsId = buffer.getLong();
            this.priceATM = buffer.getLong();
        }

        DigitalGoodsPriceChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsPriceChange(long goodsId, long priceATM) {
            this.goodsId = goodsId;
            this.priceATM = priceATM;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putLong(priceATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("priceNQT", priceATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public long getPriceATM() {
            return priceATM;
        }

    }

    final class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final long goodsId;
        private final int deltaQuantity;

        DigitalGoodsQuantityChange(ByteBuffer buffer) {
            super(buffer);
            this.goodsId = buffer.getLong();
            this.deltaQuantity = buffer.getInt();
        }

        DigitalGoodsQuantityChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.deltaQuantity = ((Long) attachmentData.get("deltaQuantity")).intValue();
        }

        public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        int getMySize() {
            return 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(deltaQuantity);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getDeltaQuantity() {
            return deltaQuantity;
        }

    }

    final class DigitalGoodsPurchase extends AbstractAttachment {

        private final long goodsId;
        private final int quantity;
        private final long priceATM;
        private final int deliveryDeadlineTimestamp;

        DigitalGoodsPurchase(ByteBuffer buffer) {
            super(buffer);
            this.goodsId = buffer.getLong();
            this.quantity = buffer.getInt();
            this.priceATM = buffer.getLong();
            this.deliveryDeadlineTimestamp = buffer.getInt();
        }

        DigitalGoodsPurchase(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
            this.deliveryDeadlineTimestamp = ((Long) attachmentData.get("deliveryDeadlineTimestamp")).intValue();
        }

        public DigitalGoodsPurchase(long goodsId, int quantity, long priceATM, int deliveryDeadlineTimestamp) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceATM = priceATM;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
        }

        @Override
        int getMySize() {
            return 8 + 4 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(quantity);
            buffer.putLong(priceATM);
            buffer.putInt(deliveryDeadlineTimestamp);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceATM);
            attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceATM() {
            return priceATM;
        }

        public int getDeliveryDeadlineTimestamp() {
            return deliveryDeadlineTimestamp;
        }

    }

    class DigitalGoodsDelivery extends AbstractAttachment {

        private final long purchaseId;
        private EncryptedData goods;
        private final long discountATM;
        private final boolean goodsIsText;

        DigitalGoodsDelivery(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.purchaseId = buffer.getLong();
            int length = buffer.getInt();
            goodsIsText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
            this.discountATM = buffer.getLong();
        }

        DigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.goods = new EncryptedData(Convert.parseHexString((String) attachmentData.get("goodsData")),
                    Convert.parseHexString((String) attachmentData.get("goodsNonce")));
            this.discountATM = attachmentData.containsKey("discountATM") ? Convert.parseLong(attachmentData.get("discountATM")) : Convert.parseLong(attachmentData.get("discountNQT"));
            this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
        }

        public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountATM) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountATM = discountATM;
            this.goodsIsText = goodsIsText;
        }

        @Override
        int getMySize() {
            return 8 + 4 + goods.getSize() + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
            buffer.put(goods.getData());
            buffer.put(goods.getNonce());
            buffer.putLong(discountATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("goodsData", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountATM);
            attachment.put("goodsIsText", goodsIsText);
        }

        @Override
        public final TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public final long getPurchaseId() {
            return purchaseId;
        }

        public final EncryptedData getGoods() {
            return goods;
        }

        final void setGoods(EncryptedData goods) {
            this.goods = goods;
        }

        int getGoodsDataLength() {
            return goods.getData().length;
        }

        public final long getDiscountATM() {
            return discountATM;
        }

        public final boolean goodsIsText() {
            return goodsIsText;
        }

    }

    final class UnencryptedDigitalGoodsDelivery extends DigitalGoodsDelivery implements Encryptable {

        private final byte[] goodsToEncrypt;
        private final byte[] recipientPublicKey;

        UnencryptedDigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            setGoods(null);
            String goodsToEncryptString = (String) attachmentData.get("goodsToEncrypt");
            this.goodsToEncrypt = goodsIsText() ? Convert.toBytes(goodsToEncryptString)
                    : Convert.parseHexString(goodsToEncryptString);
            this.recipientPublicKey = Convert.parseHexString((String) attachmentData.get("recipientPublicKey"));
        }

        public UnencryptedDigitalGoodsDelivery(long purchaseId, byte[] goodsToEncrypt, boolean goodsIsText, long discountATM, byte[] recipientPublicKey) {
            super(purchaseId, null, goodsIsText, discountATM);
            this.goodsToEncrypt = goodsToEncrypt;
            this.recipientPublicKey = recipientPublicKey;
        }

        @Override
        int getMySize() {
            if (getGoods() == null) {
                return 8 + 4 + EncryptedData.getEncryptedSize(getPlaintext()) + 8;
            }
            return super.getMySize();
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            if (getGoods() == null) {
                throw new AplException.NotYetEncryptedException("Goods not yet encrypted");
            }
            super.putMyBytes(buffer);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            if (getGoods() == null) {
                attachment.put("goodsToEncrypt", goodsIsText() ? Convert.toString(goodsToEncrypt) : Convert.toHexString(goodsToEncrypt));
                attachment.put("recipientPublicKey", Convert.toHexString(recipientPublicKey));
                attachment.put("purchase", Long.toUnsignedString(getPurchaseId()));
                attachment.put("discountNQT", getDiscountATM());
                attachment.put("goodsIsText", goodsIsText());
            } else {
                super.putMyJSON(attachment);
            }
        }

        @Override
        public void encrypt(String secretPhrase) {
            setGoods(EncryptedData.encrypt(getPlaintext(), secretPhrase, recipientPublicKey));
        }

        @Override
        int getGoodsDataLength() {
            return EncryptedData.getEncryptedDataLength(getPlaintext());
        }

        private byte[] getPlaintext() {
            return Convert.compress(goodsToEncrypt);
        }

    }

    final class DigitalGoodsFeedback extends AbstractAttachment {

        private final long purchaseId;

        DigitalGoodsFeedback(ByteBuffer buffer) {
            super(buffer);
            this.purchaseId = buffer.getLong();
        }

        DigitalGoodsFeedback(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
        }

        public DigitalGoodsFeedback(long purchaseId) {
            this.purchaseId = purchaseId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

    }

    final class DigitalGoodsRefund extends AbstractAttachment {

        private final long purchaseId;
        private final long refundATM;

        DigitalGoodsRefund(ByteBuffer buffer) {
            super(buffer);
            this.purchaseId = buffer.getLong();
            this.refundATM = buffer.getLong();
        }

        DigitalGoodsRefund(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.refundATM = attachmentData.containsKey("refundATM") ? Convert.parseLong(attachmentData.get("refundATM")) : Convert.parseLong(attachmentData.get("refundNQT"));
        }

        public DigitalGoodsRefund(long purchaseId, long refundATM) {
            this.purchaseId = purchaseId;
            this.refundATM = refundATM;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putLong(refundATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("refundNQT", refundATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

        public long getRefundATM() {
            return refundATM;
        }

    }

    final class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

        private final int period;

        AccountControlEffectiveBalanceLeasing(ByteBuffer buffer) {
            super(buffer);
            this.period = Short.toUnsignedInt(buffer.getShort());
        }

        AccountControlEffectiveBalanceLeasing(JSONObject attachmentData) {
            super(attachmentData);
            this.period = ((Long) attachmentData.get("period")).intValue();
        }

        public AccountControlEffectiveBalanceLeasing(int period) {
            this.period = period;
        }

        @Override
        int getMySize() {
            return 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putShort((short) period);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("period", period);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public int getPeriod() {
            return period;
        }
    }

    interface MonetarySystemAttachment {

        long getCurrencyId();

    }

    final class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long initialSupply;
        private final long reserveSupply;
        private final long maxSupply;
        private final int issuanceHeight;
        private final long minReservePerUnitATM;
        private final int minDifficulty;
        private final int maxDifficulty;
        private final byte ruleset;
        private final byte algorithm;
        private final byte decimals;

        MonetarySystemCurrencyIssuance(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
            this.code = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_CODE_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
            this.type = buffer.get();
            this.initialSupply = buffer.getLong();
            this.reserveSupply = buffer.getLong();
            this.maxSupply = buffer.getLong();
            this.issuanceHeight = buffer.getInt();
            this.minReservePerUnitATM = buffer.getLong();
            this.minDifficulty = buffer.get() & 0xFF;
            this.maxDifficulty = buffer.get() & 0xFF;
            this.ruleset = buffer.get();
            this.algorithm = buffer.get();
            this.decimals = buffer.get();
        }

        MonetarySystemCurrencyIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.code = (String) attachmentData.get("code");
            this.description = (String) attachmentData.get("description");
            this.type = ((Long) attachmentData.get("type")).byteValue();
            this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
            this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
            this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
            this.issuanceHeight = ((Long) attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitATM = attachmentData.containsKey("minReservePerUnitATM") ? Convert.parseLong(attachmentData.get("minReservePerUnitATM")) : Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
            this.minDifficulty = ((Long) attachmentData.get("minDifficulty")).intValue();
            this.maxDifficulty = ((Long) attachmentData.get("maxDifficulty")).intValue();
            this.ruleset = ((Long) attachmentData.get("ruleset")).byteValue();
            this.algorithm = ((Long) attachmentData.get("algorithm")).byteValue();
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply,
                                              long maxSupply, int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty,
                                              byte ruleset, byte algorithm, byte decimals) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.initialSupply = initialSupply;
            this.reserveSupply = reserveSupply;
            this.maxSupply = maxSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitATM = minReservePerUnitATM;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
            this.algorithm = algorithm;
            this.decimals = decimals;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] code = Convert.toBytes(this.code);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.put((byte) code.length);
            buffer.put(code);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(initialSupply);
            buffer.putLong(reserveSupply);
            buffer.putLong(maxSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitATM);
            buffer.put((byte) minDifficulty);
            buffer.put((byte) maxDifficulty);
            buffer.put(ruleset);
            buffer.put(algorithm);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("initialSupply", initialSupply);
            attachment.put("reserveSupply", reserveSupply);
            attachment.put("maxSupply", maxSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitATM", minReservePerUnitATM);
            attachment.put("minDifficulty", minDifficulty);
            attachment.put("maxDifficulty", maxDifficulty);
            attachment.put("ruleset", ruleset);
            attachment.put("algorithm", algorithm);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getInitialSupply() {
            return initialSupply;
        }

        public long getReserveSupply() {
            return reserveSupply;
        }

        public long getMaxSupply() {
            return maxSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitATM() {
            return minReservePerUnitATM;
        }

        public int getMinDifficulty() {
            return minDifficulty;
        }

        public int getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long amountPerUnitATM;

        MonetarySystemReserveIncrease(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.amountPerUnitATM = buffer.getLong();
        }

        MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.amountPerUnitATM = attachmentData.containsKey("amountPerUnitATM") ? Convert.parseLong(attachmentData.get("amountPerUnitATM")) : Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
        }

        public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitATM) {
            this.currencyId = currencyId;
            this.amountPerUnitATM = amountPerUnitATM;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amountPerUnitATM);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("amountPerUnitNQT", amountPerUnitATM);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_INCREASE;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getAmountPerUnitATM() {
            return amountPerUnitATM;
        }

    }

    final class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemReserveClaim(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemReserveClaim(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_CLAIM;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemCurrencyTransfer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemCurrencyTransfer(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemCurrencyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemCurrencyTransfer(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_TRANSFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }
    }

    final class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long buyRateATM;
        private final long sellRateATM;
        private final long totalBuyLimit;
        private final long totalSellLimit;
        private final long initialBuySupply;
        private final long initialSellSupply;
        private final int expirationHeight;

        MonetarySystemPublishExchangeOffer(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.buyRateATM = buffer.getLong();
            this.sellRateATM = buffer.getLong();
            this.totalBuyLimit = buffer.getLong();
            this.totalSellLimit = buffer.getLong();
            this.initialBuySupply = buffer.getLong();
            this.initialSellSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.buyRateATM = attachmentData.containsKey("buyRateATM") ? Convert.parseLong(attachmentData.get("buyRateATM")) : Convert.parseLong(attachmentData.get("buyRateNQT"));
            this.sellRateATM = attachmentData.containsKey("sellRateATM") ? Convert.parseLong(attachmentData.get("sellRateATM")) : Convert.parseLong(attachmentData.get("sellRateNQT"));
            this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
            this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
            this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
            this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
            this.expirationHeight = ((Long) attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateATM, long sellRateATM, long totalBuyLimit,
                                                  long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyRateATM = buyRateATM;
            this.sellRateATM = sellRateATM;
            this.totalBuyLimit = totalBuyLimit;
            this.totalSellLimit = totalSellLimit;
            this.initialBuySupply = initialBuySupply;
            this.initialSellSupply = initialSellSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyRateATM);
            buffer.putLong(sellRateATM);
            buffer.putLong(totalBuyLimit);
            buffer.putLong(totalSellLimit);
            buffer.putLong(initialBuySupply);
            buffer.putLong(initialSellSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("buyRateNQT", buyRateATM);
            attachment.put("sellRateNQT", sellRateATM);
            attachment.put("totalBuyLimit", totalBuyLimit);
            attachment.put("totalSellLimit", totalSellLimit);
            attachment.put("initialBuySupply", initialBuySupply);
            attachment.put("initialSellSupply", initialSellSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getBuyRateATM() {
            return buyRateATM;
        }

        public long getSellRateATM() {
            return sellRateATM;
        }

        public long getTotalBuyLimit() {
            return totalBuyLimit;
        }

        public long getTotalSellLimit() {
            return totalSellLimit;
        }

        public long getInitialBuySupply() {
            return initialBuySupply;
        }

        public long getInitialSellSupply() {
            return initialSellSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    abstract class MonetarySystemExchange extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long rateATM;
        private final long units;

        private MonetarySystemExchange(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
            this.rateATM = buffer.getLong();
            this.units = buffer.getLong();
        }

        private MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.rateATM = attachmentData.containsKey("rateATM") ? Convert.parseLong(attachmentData.get("rateATM")) : Convert.parseLong(attachmentData.get("rateNQT"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        private MonetarySystemExchange(long currencyId, long rateATM, long units) {
            this.currencyId = currencyId;
            this.rateATM = rateATM;
            this.units = units;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(rateATM);
            buffer.putLong(units);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("rateNQT", rateATM);
            attachment.put("units", units);
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateATM() {
            return rateATM;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemExchangeBuy extends MonetarySystemExchange {

        MonetarySystemExchangeBuy(ByteBuffer buffer) {
            super(buffer);
        }

        MonetarySystemExchangeBuy(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeBuy(long currencyId, long rateATM, long units) {
            super(currencyId, rateATM, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_BUY;
        }

    }

    final class MonetarySystemExchangeSell extends MonetarySystemExchange {

        MonetarySystemExchangeSell(ByteBuffer buffer) {
            super(buffer);
        }

        MonetarySystemExchangeSell(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeSell(long currencyId, long rateATM, long units) {
            super(currencyId, rateATM, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_SELL;
        }

    }

    final class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

        private final long nonce;
        private final long currencyId;
        private final long units;
        private final long counter;

        MonetarySystemCurrencyMinting(ByteBuffer buffer) {
            super(buffer);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
            this.counter = buffer.getLong();
        }

        MonetarySystemCurrencyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = Convert.parseLong(attachmentData.get("nonce"));
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
            this.counter = Convert.parseLong(attachmentData.get("counter"));
        }

        public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putLong(units);
            buffer.putLong(counter);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("nonce", nonce);
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getCounter() {
            return counter;
        }

    }

    final class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;

        MonetarySystemCurrencyDeletion(ByteBuffer buffer) {
            super(buffer);
            this.currencyId = buffer.getLong();
        }

        MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        }

        public MonetarySystemCurrencyDeletion(long currencyId) {
            this.currencyId = currencyId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_DELETION;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }
    }

    final class ShufflingCreation extends AbstractAttachment {

        private final long holdingId;
        private final HoldingType holdingType;
        private final long amount;
        private final byte participantCount;
        private final short registrationPeriod;

        ShufflingCreation(ByteBuffer buffer) {
            super(buffer);
            this.holdingId = buffer.getLong();
            this.holdingType = HoldingType.get(buffer.get());
            this.amount = buffer.getLong();
            this.participantCount = buffer.get();
            this.registrationPeriod = buffer.getShort();
        }

        ShufflingCreation(JSONObject attachmentData) {
            super(attachmentData);
            this.holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.holdingType = HoldingType.get(((Long) attachmentData.get("holdingType")).byteValue());
            this.amount = Convert.parseLong(attachmentData.get("amount"));
            this.participantCount = ((Long) attachmentData.get("participantCount")).byteValue();
            this.registrationPeriod = ((Long) attachmentData.get("registrationPeriod")).shortValue();
        }

        public ShufflingCreation(long holdingId, HoldingType holdingType, long amount, byte participantCount, short registrationPeriod) {
            this.holdingId = holdingId;
            this.holdingType = holdingType;
            this.amount = amount;
            this.participantCount = participantCount;
            this.registrationPeriod = registrationPeriod;
        }

        @Override
        int getMySize() {
            return 8 + 1 + 8 + 1 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(holdingId);
            buffer.put(holdingType.getCode());
            buffer.putLong(amount);
            buffer.put(participantCount);
            buffer.putShort(registrationPeriod);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("holding", Long.toUnsignedString(holdingId));
            attachment.put("holdingType", holdingType.getCode());
            attachment.put("amount", amount);
            attachment.put("participantCount", participantCount);
            attachment.put("registrationPeriod", registrationPeriod);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CREATION;
        }

        public long getHoldingId() {
            return holdingId;
        }

        public HoldingType getHoldingType() {
            return holdingType;
        }

        public long getAmount() {
            return amount;
        }

        public byte getParticipantCount() {
            return participantCount;
        }

        public short getRegistrationPeriod() {
            return registrationPeriod;
        }
    }

    interface ShufflingAttachment extends Attachment {

        long getShufflingId();

        byte[] getShufflingStateHash();

    }

    abstract class AbstractShufflingAttachment extends AbstractAttachment implements ShufflingAttachment {

        private final long shufflingId;
        private final byte[] shufflingStateHash;

        private AbstractShufflingAttachment(ByteBuffer buffer) {
            super(buffer);
            this.shufflingId = buffer.getLong();
            this.shufflingStateHash = new byte[32];
            buffer.get(this.shufflingStateHash);
        }

        private AbstractShufflingAttachment(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String) attachmentData.get("shuffling"));
            this.shufflingStateHash = Convert.parseHexString((String) attachmentData.get("shufflingStateHash"));
        }

        private AbstractShufflingAttachment(long shufflingId, byte[] shufflingStateHash) {
            this.shufflingId = shufflingId;
            this.shufflingStateHash = shufflingStateHash;
        }

        @Override
        int getMySize() {
            return 8 + 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.put(shufflingStateHash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Long.toUnsignedString(shufflingId));
            attachment.put("shufflingStateHash", Convert.toHexString(shufflingStateHash));
        }

        @Override
        public final long getShufflingId() {
            return shufflingId;
        }

        @Override
        public final byte[] getShufflingStateHash() {
            return shufflingStateHash;
        }

    }

    final class ShufflingRegistration extends AbstractAttachment implements ShufflingAttachment {

        private final byte[] shufflingFullHash;

        ShufflingRegistration(ByteBuffer buffer) {
            super(buffer);
            this.shufflingFullHash = new byte[32];
            buffer.get(this.shufflingFullHash);
        }

        ShufflingRegistration(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingFullHash = Convert.parseHexString((String) attachmentData.get("shufflingFullHash"));
        }

        public ShufflingRegistration(byte[] shufflingFullHash) {
            this.shufflingFullHash = shufflingFullHash;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_REGISTRATION;
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(shufflingFullHash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("shufflingFullHash", Convert.toHexString(shufflingFullHash));
        }

        @Override
        public long getShufflingId() {
            return Convert.fullHashToId(shufflingFullHash);
        }

        @Override
        public byte[] getShufflingStateHash() {
            return shufflingFullHash;
        }

    }

    final class ShufflingProcessing extends AbstractShufflingAttachment implements Prunable {

        private static final byte[] emptyDataHash = Crypto.sha256().digest();

        static ShufflingProcessing parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(ShufflingTransaction.SHUFFLING_PROCESSING.getName(), attachmentData)) {
                return null;
            }
            return new ShufflingProcessing(attachmentData);
        }

        private volatile byte[][] data;
        private final byte[] hash;

        ShufflingProcessing(ByteBuffer buffer) {
            super(buffer);
            this.hash = new byte[32];
            buffer.get(hash);
            this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
        }

        ShufflingProcessing(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("data");
            if (jsonArray != null) {
                this.data = new byte[jsonArray.size()][];
                for (int i = 0; i < this.data.length; i++) {
                    this.data[i] = Convert.parseHexString((String) jsonArray.get(i));
                }
                this.hash = null;
            } else {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
                this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
            }
        }

        ShufflingProcessing(long shufflingId, byte[][] data, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.data = data;
            this.hash = null;
        }

        @Override
        int getMyFullSize() {
            int size = super.getMySize();
            if (data != null) {
                size += 1;
                for (byte[] bytes : data) {
                    size += 4;
                    size += bytes.length;
                }
            }
            return size / 2; // just lie
        }

        @Override
        int getMySize() {
            return super.getMySize() + 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put(getHash());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            if (data != null) {
                JSONArray jsonArray = new JSONArray();
                attachment.put("data", jsonArray);
                for (byte[] bytes : data) {
                    jsonArray.add(Convert.toHexString(bytes));
                }
            }
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_PROCESSING;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            } else if (data != null) {
                MessageDigest digest = Crypto.sha256();
                for (byte[] bytes : data) {
                    digest.update(bytes);
                }
                return digest.digest();
            } else {
                throw new IllegalStateException("Both hash and data are null");
            }
        }

        public byte[][] getData() {
            return data;
        }

        @Override
        void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                data = ShufflingParticipant.getData(getShufflingId(), transaction.getSenderId());
            }
        }

        @Override
        public boolean hasPrunableData() {
            return data != null;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            ShufflingParticipant.restoreData(getShufflingId(), transaction.getSenderId(), getData(), transaction.getTimestamp(), height);
        }

    }

    final class ShufflingRecipients extends AbstractShufflingAttachment {

        private final byte[][] recipientPublicKeys;

        ShufflingRecipients(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count < 0) {
                throw new AplException.NotValidException("Invalid data count " + count);
            }
            this.recipientPublicKeys = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.recipientPublicKeys[i] = new byte[32];
                buffer.get(this.recipientPublicKeys[i]);
            }
        }

        ShufflingRecipients(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("recipientPublicKeys");
            this.recipientPublicKeys = new byte[jsonArray.size()][];
            for (int i = 0; i < this.recipientPublicKeys.length; i++) {
                this.recipientPublicKeys[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
        }

        ShufflingRecipients(long shufflingId, byte[][] recipientPublicKeys, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.recipientPublicKeys = recipientPublicKeys;
        }

        @Override
        int getMySize() {
            int size = super.getMySize();
            size += 1;
            size += 32 * recipientPublicKeys.length;
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte) recipientPublicKeys.length);
            for (byte[] bytes : recipientPublicKeys) {
                buffer.put(bytes);
            }
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("recipientPublicKeys", jsonArray);
            for (byte[] bytes : recipientPublicKeys) {
                jsonArray.add(Convert.toHexString(bytes));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_RECIPIENTS;
        }

        public byte[][] getRecipientPublicKeys() {
            return recipientPublicKeys;
        }

    }

    final class ShufflingVerification extends AbstractShufflingAttachment {

        ShufflingVerification(ByteBuffer buffer) {
            super(buffer);
        }

        ShufflingVerification(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ShufflingVerification(long shufflingId, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_VERIFICATION;
        }

    }

    final class ShufflingCancellation extends AbstractShufflingAttachment {

        private final byte[][] blameData;
        private final byte[][] keySeeds;
        private final long cancellingAccountId;

        ShufflingCancellation(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new AplException.NotValidException("Invalid data count " + count);
            }
            this.blameData = new byte[count][];
            for (int i = 0; i < count; i++) {
                int size = buffer.getInt();
                if (size > Constants.MAX_PAYLOAD_LENGTH) {
                    throw new AplException.NotValidException("Invalid data size " + size);
                }
                this.blameData[i] = new byte[size];
                buffer.get(this.blameData[i]);
            }
            count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new AplException.NotValidException("Invalid keySeeds count " + count);
            }
            this.keySeeds = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.keySeeds[i] = new byte[32];
                buffer.get(this.keySeeds[i]);
            }
            this.cancellingAccountId = buffer.getLong();
        }

        ShufflingCancellation(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("blameData");
            this.blameData = new byte[jsonArray.size()][];
            for (int i = 0; i < this.blameData.length; i++) {
                this.blameData[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
            jsonArray = (JSONArray) attachmentData.get("keySeeds");
            this.keySeeds = new byte[jsonArray.size()][];
            for (int i = 0; i < this.keySeeds.length; i++) {
                this.keySeeds[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
            this.cancellingAccountId = Convert.parseUnsignedLong((String) attachmentData.get("cancellingAccount"));
        }

        ShufflingCancellation(long shufflingId, byte[][] blameData, byte[][] keySeeds, byte[] shufflingStateHash, long cancellingAccountId) {
            super(shufflingId, shufflingStateHash);
            this.blameData = blameData;
            this.keySeeds = keySeeds;
            this.cancellingAccountId = cancellingAccountId;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CANCELLATION;
        }

        @Override
        int getMySize() {
            int size = super.getMySize();
            size += 1;
            for (byte[] bytes : blameData) {
                size += 4;
                size += bytes.length;
            }
            size += 1;
            size += 32 * keySeeds.length;
            size += 8;
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte) blameData.length);
            for (byte[] bytes : blameData) {
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            }
            buffer.put((byte) keySeeds.length);
            for (byte[] bytes : keySeeds) {
                buffer.put(bytes);
            }
            buffer.putLong(cancellingAccountId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("blameData", jsonArray);
            for (byte[] bytes : blameData) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            jsonArray = new JSONArray();
            attachment.put("keySeeds", jsonArray);
            for (byte[] bytes : keySeeds) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            if (cancellingAccountId != 0) {
                attachment.put("cancellingAccount", Long.toUnsignedString(cancellingAccountId));
            }
        }

        public byte[][] getBlameData() {
            return blameData;
        }

        public byte[][] getKeySeeds() {
            return keySeeds;
        }

        public long getCancellingAccountId() {
            return cancellingAccountId;
        }

        byte[] getHash() {
            MessageDigest digest = Crypto.sha256();
            for (byte[] bytes : blameData) {
                digest.update(bytes);
            }
            return digest.digest();
        }

    }

    abstract class TaggedDataAttachment extends AbstractAttachment implements Prunable {

        private final String name;
        private final String description;
        private final String tags;
        private final String type;
        private final String channel;
        private final boolean isText;
        private final String filename;
        private final byte[] data;
        private volatile TaggedData taggedData;

        private TaggedDataAttachment(ByteBuffer buffer) {
            super(buffer);
            this.name = null;
            this.description = null;
            this.tags = null;
            this.type = null;
            this.channel = null;
            this.isText = false;
            this.filename = null;
            this.data = null;
        }

        private TaggedDataAttachment(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON != null) {
                this.name = (String) attachmentData.get("name");
                this.description = (String) attachmentData.get("description");
                this.tags = (String) attachmentData.get("tags");
                this.type = (String) attachmentData.get("type");
                this.channel = Convert.nullToEmpty((String) attachmentData.get("channel"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("isText"));
                this.data = isText ? Convert.toBytes(dataJSON) : Convert.parseHexString(dataJSON);
                this.filename = (String) attachmentData.get("filename");
            } else {
                this.name = null;
                this.description = null;
                this.tags = null;
                this.type = null;
                this.channel = null;
                this.isText = false;
                this.filename = null;
                this.data = null;
            }

        }

        private TaggedDataAttachment(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.type = type;
            this.channel = channel;
            this.isText = isText;
            this.data = data;
            this.filename = filename;
        }

        @Override
        final int getMyFullSize() {
            if (getData() == null) {
                return 0;
            }
            return Convert.toBytes(getName()).length + Convert.toBytes(getDescription()).length + Convert.toBytes(getType()).length
                    + Convert.toBytes(getChannel()).length + Convert.toBytes(getTags()).length + Convert.toBytes(getFilename()).length + getData().length;
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            if (taggedData != null) {
                attachment.put("name", taggedData.getName());
                attachment.put("description", taggedData.getDescription());
                attachment.put("tags", taggedData.getTags());
                attachment.put("type", taggedData.getType());
                attachment.put("channel", taggedData.getChannel());
                attachment.put("isText", taggedData.isText());
                attachment.put("filename", taggedData.getFilename());
                attachment.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
            } else if (data != null) {
                attachment.put("name", name);
                attachment.put("description", description);
                attachment.put("tags", tags);
                attachment.put("type", type);
                attachment.put("channel", channel);
                attachment.put("isText", isText);
                attachment.put("filename", filename);
                attachment.put("data", isText ? Convert.toString(data) : Convert.toHexString(data));
            }
        }

        @Override
        public byte[] getHash() {
            if (data == null) {
                return null;
            }
            MessageDigest digest = Crypto.sha256();
            digest.update(Convert.toBytes(name));
            digest.update(Convert.toBytes(description));
            digest.update(Convert.toBytes(tags));
            digest.update(Convert.toBytes(type));
            digest.update(Convert.toBytes(channel));
            digest.update((byte) (isText ? 1 : 0));
            digest.update(Convert.toBytes(filename));
            digest.update(data);
            return digest.digest();
        }

        public final String getName() {
            if (taggedData != null) {
                return taggedData.getName();
            }
            return name;
        }

        public final String getDescription() {
            if (taggedData != null) {
                return taggedData.getDescription();
            }
            return description;
        }

        public final String getTags() {
            if (taggedData != null) {
                return taggedData.getTags();
            }
            return tags;
        }

        public final String getType() {
            if (taggedData != null) {
                return taggedData.getType();
            }
            return type;
        }

        public final String getChannel() {
            if (taggedData != null) {
                return taggedData.getChannel();
            }
            return channel;
        }

        public final boolean isText() {
            if (taggedData != null) {
                return taggedData.isText();
            }
            return isText;
        }

        public final String getFilename() {
            if (taggedData != null) {
                return taggedData.getFilename();
            }
            return filename;
        }

        public final byte[] getData() {
            if (taggedData != null) {
                return taggedData.getData();
            }
            return data;
        }

        @Override
        void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && taggedData == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                taggedData = TaggedData.getData(getTaggedDataId(transaction));
            }
        }

        @Override
        public boolean hasPrunableData() {
            return (taggedData != null || data != null);
        }

        abstract long getTaggedDataId(Transaction transaction);

    }

    final class TaggedDataUpload extends TaggedDataAttachment {

        static TaggedDataUpload parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataUpload(attachmentData);
        }

        private final byte[] hash;

        TaggedDataUpload(ByteBuffer buffer) {
            super(buffer);
            this.hash = new byte[32];
            buffer.get(hash);
        }

        TaggedDataUpload(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON == null) {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
            } else {
                this.hash = null;
            }
        }

        public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText,
                                String filename, byte[] data) throws AplException.NotValidException {
            super(name, description, tags, type, channel, isText, filename, data);
            this.hash = null;
            if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
                throw new AplException.NotValidException("Data is not UTF-8 text");
            }
        }

        @Override
        int getMySize() {
            return 32;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put(getHash());
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_UPLOAD;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            }
            return super.getHash();
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return transaction.getId();
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            TaggedData.restore(transaction, this, blockTimestamp, height);
        }

    }

    final class TaggedDataExtend extends TaggedDataAttachment {

        static TaggedDataExtend parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataExtend(attachmentData);
        }

        private volatile byte[] hash;
        private final long taggedDataId;
        private final boolean jsonIsPruned;

        TaggedDataExtend(ByteBuffer buffer) {
            super(buffer);
            this.taggedDataId = buffer.getLong();
            this.jsonIsPruned = false;
        }

        TaggedDataExtend(JSONObject attachmentData) {
            super(attachmentData);
            this.taggedDataId = Convert.parseUnsignedLong((String) attachmentData.get("taggedData"));
            this.jsonIsPruned = attachmentData.get("data") == null;
        }

        public TaggedDataExtend(TaggedData taggedData) {
            super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(),
                    taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
            this.taggedDataId = taggedData.getId();
            this.jsonIsPruned = false;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(taggedDataId);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_EXTEND;
        }

        public long getTaggedDataId() {
            return taggedDataId;
        }

        @Override
        public byte[] getHash() {
            if (hash == null) {
                hash = super.getHash();
            }
            if (hash == null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload) TransactionDb.findTransaction(taggedDataId).getAttachment();
                hash = taggedDataUpload.getHash();
            }
            return hash;
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return taggedDataId;
        }

        boolean jsonIsPruned() {
            return jsonIsPruned;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        }

    }

    final class SetPhasingOnly extends AbstractAttachment {

        private final PhasingParams phasingParams;
        private final long maxFees;
        private final short minDuration;
        private final short maxDuration;

        public SetPhasingOnly(PhasingParams params, long maxFees, short minDuration, short maxDuration) {
            phasingParams = params;
            this.maxFees = maxFees;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        SetPhasingOnly(ByteBuffer buffer) {
            super(buffer);
            phasingParams = new PhasingParams(buffer);
            maxFees = buffer.getLong();
            minDuration = buffer.getShort();
            maxDuration = buffer.getShort();
        }

        SetPhasingOnly(JSONObject attachmentData) {
            super(attachmentData);
            JSONObject phasingControlParams = (JSONObject) attachmentData.get("phasingControlParams");
            phasingParams = new PhasingParams(phasingControlParams);
            maxFees = Convert.parseLong(attachmentData.get("controlMaxFees"));
            minDuration = ((Long) attachmentData.get("controlMinDuration")).shortValue();
            maxDuration = ((Long) attachmentData.get("controlMaxDuration")).shortValue();
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.SET_PHASING_ONLY;
        }

        @Override
        int getMySize() {
            return phasingParams.getMySize() + 8 + 2 + 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            phasingParams.putMyBytes(buffer);
            buffer.putLong(maxFees);
            buffer.putShort(minDuration);
            buffer.putShort(maxDuration);
        }

        @Override
        void putMyJSON(JSONObject json) {
            JSONObject phasingControlParams = new JSONObject();
            phasingParams.putMyJSON(phasingControlParams);
            json.put("phasingControlParams", phasingControlParams);
            json.put("controlMaxFees", maxFees);
            json.put("controlMinDuration", minDuration);
            json.put("controlMaxDuration", maxDuration);
        }

        public PhasingParams getPhasingParams() {
            return phasingParams;
        }

        public long getMaxFees() {
            return maxFees;
        }

        public short getMinDuration() {
            return minDuration;
        }

        public short getMaxDuration() {
            return maxDuration;
        }

    }

    abstract class UpdateAttachment extends AbstractAttachment {

        private final Platform platform;
        private final Architecture architecture;
        private final byte[] url;
        private final Version version;
        private final byte[] hash;

        UpdateAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
            platform = Platform.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_PLATFORM_LENGTH).trim());
            architecture = Architecture.valueOf(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_ARCHITECTURE_LENGTH).trim());
            int urlLength = buffer.getShort();
            url = new byte[urlLength];
            buffer.get(url);
            version = Version.from(Convert.readString(buffer, buffer.get(), Constants.MAX_UPDATE_VERSION_LENGTH).trim());
            int hashLength = buffer.getShort();
            hash = new byte[hashLength];
            buffer.get(hash);
        }

        UpdateAttachment(JSONObject attachmentData) {
            super(attachmentData);
            platform = Platform.valueOf(Convert.nullToEmpty((String) attachmentData.get("platform")).trim());
            architecture = Architecture.valueOf(Convert.nullToEmpty((String) attachmentData.get("architecture")).trim());
            url = Convert.parseHexString(Convert.nullToEmpty(((String) attachmentData.get("url")).trim()));
            version = Version.from(Convert.nullToEmpty((String) attachmentData.get("version")).trim());
            hash = Convert.parseHexString(Convert.nullToEmpty((String) attachmentData.get("hash")).trim());
        }

        public UpdateAttachment(Platform platform, Architecture architecture, byte[] url, Version version, byte[] hash) {
            this.platform = platform;
            this.architecture = architecture;
            this.url = url;
            this.version = version;
            this.hash = hash;
        }

        public abstract Level getLevel();

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(platform.name()).length + 1 + Convert.toBytes(architecture.name()).length
                    + 2 + url.length + 1 + Convert.toBytes(version.toString()).length + 2 + hash.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] platform = Convert.toBytes(this.platform.toString());
            byte[] architecture = Convert.toBytes(this.architecture.toString());
            byte[] version = Convert.toBytes(this.version.toString());
            buffer.put((byte) platform.length);
            buffer.put(platform);
            buffer.put((byte) architecture.length);
            buffer.put(architecture);
            buffer.putShort((short) url.length);
            buffer.put(url);
            buffer.put((byte) version.length);
            buffer.put(version);
            buffer.putShort((short) hash.length);
            buffer.put(hash);
        }

        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("platform", platform.toString());
            attachment.put("architecture", architecture.toString());
            attachment.put("url", Convert.toHexString(url));
            attachment.put("version", version.toString());
            attachment.put("hash", Convert.toHexString(hash));
        }

        public Platform getPlatform() {
            return platform;
        }

        public Architecture getArchitecture() {
            return architecture;
        }

        public byte[] getUrl() {
            return url;
        }

        public Version getAppVersion() {
            return version;
        }

        public byte[] getHash() {
            return hash;
        }

        public static UpdateAttachment getAttachment(Platform platform, Architecture architecture, byte[] url, Version version, byte[] hash, byte level) {
            if (level == TransactionType.Update.CRITICAL.getSubtype()) {
                return new CriticalUpdate(platform, architecture, url, version, hash);
            } else if (level == TransactionType.Update.IMPORTANT.getSubtype()) {
                return new ImportantUpdate(platform, architecture, url, version, hash);
            } else if (level == TransactionType.Update.MINOR.getSubtype()) {
                return new MinorUpdate(platform, architecture, url, version, hash);
            }
            return null;
        }
    }

    final class CriticalUpdate extends UpdateAttachment {
        CriticalUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        CriticalUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public CriticalUpdate(Platform platform, Architecture architecture, byte[] url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public Level getLevel() {
            return Level.CRITICAL;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Update.CRITICAL;
        }
    }

    final class ImportantUpdate extends UpdateAttachment {
        ImportantUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        ImportantUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ImportantUpdate(Platform platform, Architecture architecture, byte[] url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public Level getLevel() {
            return Level.IMPORTANT;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Update.IMPORTANT;
        }
    }

    final class MinorUpdate extends UpdateAttachment {
        MinorUpdate(ByteBuffer buffer) throws AplException.NotValidException {
            super(buffer);
        }

        MinorUpdate(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MinorUpdate(Platform platform, Architecture architecture, byte[] url, Version version, byte[] hash) {
            super(platform, architecture, url, version, hash);
        }

        @Override
        public Level getLevel() {
            return Level.MINOR;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Update.MINOR;
        }
    }

}
