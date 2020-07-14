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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingVerificationAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ShufflingTransaction extends TransactionType {

    private static final byte SUBTYPE_SHUFFLING_CREATION = 0;
    public static final TransactionType SHUFFLING_CREATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_CREATION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_REGISTRATION;
        }

        @Override
        public String getName() {
            return "ShufflingCreation";
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return new ShufflingCreation(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new ShufflingCreation(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            long amount = attachment.getAmount();
            if (holdingType == HoldingType.APL) {
                BlockchainConfig blockchainConfig = lookupBlockchainConfig();
                if (amount < blockchainConfig.getShufflingDepositAtm() || amount > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                    throw new AplException.NotValidException("Invalid ATM amount " + amount
                        + ", minimum is " + blockchainConfig.getShufflingDepositAtm());
                }
            } else if (holdingType == HoldingType.ASSET) {
                Asset asset = lookupAssetService().getAsset(attachment.getHoldingId());
                if (asset == null) {
                    throw new AplException.NotCurrentlyValidException("Unknown asset " + Long.toUnsignedString(attachment.getHoldingId()));
                }
                if (amount <= 0 || amount > asset.getInitialQuantityATU()) {
                    throw new AplException.NotValidException("Invalid asset quantity " + amount);
                }
            } else if (holdingType == HoldingType.CURRENCY) {
                Currency currency = lookupCurrencyService().getCurrency(attachment.getHoldingId());
                CurrencyType.validate(currency, transaction);
                if (!lookupCurrencyService().isActive(currency)) {
                    throw new AplException.NotCurrentlyValidException("Currency is not active: " + currency.getCode());
                }
                if (amount <= 0 || amount > Constants.MAX_CURRENCY_TOTAL_SUPPLY) {
                    throw new AplException.NotValidException("Invalid currency amount " + amount);
                }
            } else {
                throw new RuntimeException("Unsupported holding type " + holdingType);
            }
            if (attachment.getParticipantCount() < Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS
                || attachment.getParticipantCount() > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS) {
                throw new AplException.NotValidException(String.format("Number of participants %d is not between %d and %d",
                    attachment.getParticipantCount(), Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS, Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS));
            }
            if (attachment.getRegistrationPeriod() < 1 || attachment.getRegistrationPeriod() > Constants.MAX_SHUFFLING_REGISTRATION_PERIOD) {
                throw new AplException.NotValidException("Invalid registration period: " + attachment.getRegistrationPeriod());
            }
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            if (holdingType != HoldingType.APL) {
                BlockchainConfig blockchainConfig = lookupBlockchainConfig();
                if (holdingType.getUnconfirmedBalance(senderAccount, attachment.getHoldingId()) >= attachment.getAmount()
                    && senderAccount.getUnconfirmedBalanceATM() >= lookupBlockchainConfig().getShufflingDepositAtm()) {
                    holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), -attachment.getAmount());
                    lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -lookupBlockchainConfig().getShufflingDepositAtm());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getAmount()) {
                    lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -attachment.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
            lookupShufflingService().addShuffling(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            if (holdingType != HoldingType.APL) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), attachment.getAmount());
                lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), lookupBlockchainConfig().getShufflingDepositAtm());
            } else {
                lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAmount());
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
            if (attachment.getHoldingType() != HoldingType.CURRENCY) {
                return false;
            }
            Currency currency = lookupCurrencyService().getCurrency(attachment.getHoldingId());
            String nameLower = currency.getName().toLowerCase();
            String codeLower = currency.getCode().toLowerCase();
            boolean isDuplicate = TransactionType.isDuplicate(MonetarySystem.CURRENCY_ISSUANCE, nameLower, duplicates, false);
            if (!nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || TransactionType.isDuplicate(MonetarySystem.CURRENCY_ISSUANCE, codeLower, duplicates, false);
            }
            return isDuplicate;
        }

    };
    private static final byte SUBTYPE_SHUFFLING_REGISTRATION = 1;
    public static final TransactionType SHUFFLING_REGISTRATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_REGISTRATION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_REGISTRATION;
        }

        @Override
        public String getName() {
            return "ShufflingRegistration";
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return new ShufflingRegistration(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new ShufflingRegistration(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            byte[] shufflingStateHash = lookupShufflingService().getStageHash(shuffling);
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            if (shuffling.getStage() != ShufflingStage.REGISTRATION) {
                throw new AplException.NotCurrentlyValidException("Shuffling registration has ended for " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (lookupShufflingService().getParticipant(shuffling.getId(), transaction.getSenderId()) != null) {
                throw new AplException.NotCurrentlyValidException(String.format("Account %s is already registered for shuffling %s",
                    Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (lookupBlockchain().getHeight() + shuffling.getBlocksRemaining() <= attachment.getFinishValidationHeight(transaction)) {
                throw new AplException.NotCurrentlyValidException("Shuffling registration finishes in " + shuffling.getBlocksRemaining() + " blocks");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            return TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
                Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true)
                || TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
                Long.toUnsignedString(shuffling.getId()), duplicates, shuffling.getParticipantCount() - shuffling.getRegistrantCount());
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            HoldingType holdingType = shuffling.getHoldingType();
            if (holdingType != HoldingType.APL) {
                BlockchainConfig blockchainConfig = lookupBlockchainConfig();
                if (holdingType.getUnconfirmedBalance(senderAccount, shuffling.getHoldingId()) >= shuffling.getAmount()
                    && senderAccount.getUnconfirmedBalanceATM() >= blockchainConfig.getShufflingDepositAtm()) {
                    holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), -shuffling.getAmount());
                    lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -blockchainConfig.getShufflingDepositAtm());
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceATM() >= shuffling.getAmount()) {
                    lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -shuffling.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            lookupShufflingService().addParticipant(shuffling, transaction.getSenderId());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            HoldingType holdingType = shuffling.getHoldingType();
            if (holdingType != HoldingType.APL) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), shuffling.getAmount());
                lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), lookupBlockchainConfig().getShufflingDepositAtm());
            } else {
                lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getAmount());
            }
        }

    };
    private static final byte SUBTYPE_SHUFFLING_PROCESSING = 2;
    private static final byte SUBTYPE_SHUFFLING_RECIPIENTS = 3;
    private static final byte SUBTYPE_SHUFFLING_VERIFICATION = 4;
    public static final TransactionType SHUFFLING_VERIFICATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_VERIFICATION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingVerification";
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return new ShufflingVerificationAttachment(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new ShufflingVerificationAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingVerificationAttachment attachment = (ShufflingVerificationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != ShufflingStage.VERIFICATION) {
                throw new AplException.NotCurrentlyValidException("Shuffling not in verification stage: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            ShufflingParticipant participant = lookupShufflingService().getParticipant(shuffling.getId(), transaction.getSenderId());
            if (participant == null) {
                throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                    Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipantState.VERIFIED)) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling participant %s in state %s cannot become verified",
                    Long.toUnsignedString(attachment.getShufflingId()), participant.getState()));
            }
            if (participant.getIndex() == shuffling.getParticipantCount() - 1) {
                throw new AplException.NotValidException("Last participant cannot submit verification transaction");
            }
            byte[] shufflingStateHash = lookupShufflingService().getStageHash(shuffling);
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingVerificationAttachment attachment = (ShufflingVerificationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            return TransactionType.isDuplicate(SHUFFLING_VERIFICATION,
                Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingVerificationAttachment attachment = (ShufflingVerificationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            lookupShufflingService().verify(shuffling, transaction.getSenderId());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isPhasable() {
            return false;
        }

    };
    private static final byte SUBTYPE_SHUFFLING_CANCELLATION = 5;
    private final static Fee SHUFFLING_PROCESSING_FEE = new Fee.ConstantFee(10 * Constants.ONE_APL);
    public static final TransactionType SHUFFLING_PROCESSING = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_PROCESSING;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingProcessing";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_PROCESSING_FEE;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ShufflingProcessingAttachment(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ShufflingProcessingAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != ShufflingStage.PROCESSING) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                    Long.toUnsignedString(attachment.getShufflingId())));
            }
            ShufflingParticipant participant = lookupShufflingService().getParticipant(shuffling.getId(), transaction.getSenderId());
            if (participant == null) {
                throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                    Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipantState.PROCESSED)) {
                throw new AplException.NotCurrentlyValidException(String.format("Participant %s processing already complete",
                    Long.toUnsignedString(transaction.getSenderId())));
            }
            if (participant.getAccountId() != shuffling.getAssigneeAccountId()) {
                throw new AplException.NotCurrentlyValidException(String.format("Participant %s is not currently assigned to process shuffling %s",
                    Long.toUnsignedString(participant.getAccountId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (participant.getNextAccountId() == 0) {
                throw new AplException.NotValidException(String.format("Participant %s is last in shuffle",
                    Long.toUnsignedString(transaction.getSenderId())));
            }
            byte[] shufflingStateHash = lookupShufflingService().getStageHash(shuffling);
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            byte[][] data = attachment.getData();
            if (data == null && lookupTimeService().getEpochTime() - transaction.getTimestamp() < lookupBlockchainConfig().getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            if (data != null) {
                if (data.length != participant.getIndex() + 1 && data.length != 0) {
                    throw new AplException.NotValidException(String.format("Invalid number of encrypted data %d for participant number %d",
                        data.length, participant.getIndex()));
                }
                byte[] previous = null;
                for (byte[] bytes : data) {
                    if (bytes.length != 32 + 64 * (shuffling.getParticipantCount() - participant.getIndex() - 1)) {
                        throw new AplException.NotValidException("Invalid encrypted data length " + bytes.length);
                    }
                    if (previous != null && Convert.byteArrayComparator.compare(previous, bytes) >= 0) {
                        throw new AplException.NotValidException("Duplicate or unsorted encrypted data");
                    }
                    previous = bytes;
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            return TransactionType.isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            lookupShufflingService().updateParticipantData(shuffling, transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isPhasable() {
            return false;
        }

        @Override
        public boolean isPruned(long transactionId) {
            Transaction transaction = lookupBlockchain().getTransaction(transactionId);

            ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
            return lookupShufflingService().getData(attachment.getShufflingId(), transaction.getSenderId()) == null;
        }

    };
    public static final TransactionType SHUFFLING_CANCELLATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_CANCELLATION;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingCancellation";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_PROCESSING_FEE;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ShufflingCancellationAttachment(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new ShufflingCancellationAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingCancellationAttachment attachment = (ShufflingCancellationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            long cancellingAccountId = attachment.getCancellingAccountId();
            if (cancellingAccountId == 0 && !shuffling.getStage().canBecome(ShufflingStage.BLAME)) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling in state %s cannot be cancelled", shuffling.getStage()));
            }
            if (cancellingAccountId != 0 && cancellingAccountId != shuffling.getAssigneeAccountId()) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling %s is not currently being cancelled by account %s",
                    Long.toUnsignedString(shuffling.getId()), Long.toUnsignedString(cancellingAccountId)));
            }
            ShufflingParticipant participant = lookupShufflingService().getParticipant(shuffling.getId(), transaction.getSenderId());
            if (participant == null) {
                throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                    Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipantState.CANCELLED)) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling participant %s in state %s cannot submit cancellation",
                    Long.toUnsignedString(attachment.getShufflingId()), participant.getState()));
            }
            if (participant.getIndex() == shuffling.getParticipantCount() - 1) {
                throw new AplException.NotValidException("Last participant cannot submit cancellation transaction");
            }
            byte[] shufflingStateHash = lookupShufflingService().getStageHash(shuffling);
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }

            Blockchain blockchain = lookupBlockchain();
            if (!blockchain.hasTransactionByFullHash(participant.getDataTransactionFullHash(),
                blockchain.getHeight())) {
                throw new AplException.NotCurrentlyValidException("Invalid data transaction full hash");
            }
            byte[] dataHash = participant.getDataHash();
            if (dataHash == null || !Arrays.equals(dataHash, attachment.getHash())) {
                throw new AplException.NotValidException("Blame data hash doesn't match processing data hash");
            }
            byte[][] keySeeds = attachment.getKeySeeds();
            if (keySeeds.length != shuffling.getParticipantCount() - participant.getIndex() - 1) {
                throw new AplException.NotValidException("Invalid number of revealed keySeeds: " + keySeeds.length);
            }
            for (byte[] keySeed : keySeeds) {
                if (keySeed.length != 32) {
                    throw new AplException.NotValidException("Invalid keySeed: " + Convert.toHexString(keySeed));
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingCancellationAttachment attachment = (ShufflingCancellationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            return TransactionType.isDuplicate(SHUFFLING_VERIFICATION, // use VERIFICATION for unique type
                Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingCancellationAttachment attachment = (ShufflingCancellationAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            ShufflingParticipant participant = lookupShufflingService().getParticipant(shuffling.getId(), senderAccount.getId());
            lookupShufflingService().cancelBy(shuffling, participant, attachment.getBlameData(), attachment.getKeySeeds());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isPhasable() {
            return false;
        }
    };
    private final static Fee SHUFFLING_RECIPIENTS_FEE = new Fee.ConstantFee(11 * Constants.ONE_APL);
    public static final TransactionType SHUFFLING_RECIPIENTS = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_RECIPIENTS;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingRecipients";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_RECIPIENTS_FEE;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ShufflingRecipientsAttachment(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new ShufflingRecipientsAttachment(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            ShufflingRecipientsAttachment attachment = (ShufflingRecipientsAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != ShufflingStage.PROCESSING) {
                throw new AplException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                    Long.toUnsignedString(attachment.getShufflingId())));
            }
            ShufflingParticipant participant = lookupShufflingService().getParticipant(shuffling.getId(), transaction.getSenderId());
            if (participant == null) {
                throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                    Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (participant.getNextAccountId() != 0) {
                throw new AplException.NotValidException(String.format("Participant %s is not last in shuffle",
                    Long.toUnsignedString(transaction.getSenderId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipantState.PROCESSED)) {
                throw new AplException.NotCurrentlyValidException(String.format("Participant %s processing already complete",
                    Long.toUnsignedString(transaction.getSenderId())));
            }
            if (participant.getAccountId() != shuffling.getAssigneeAccountId()) {
                throw new AplException.NotCurrentlyValidException(String.format("Participant %s is not currently assigned to process shuffling %s",
                    Long.toUnsignedString(participant.getAccountId()), Long.toUnsignedString(shuffling.getId())));
            }
            byte[] shufflingStateHash = lookupShufflingService().getStageHash(shuffling);
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            byte[][] recipientPublicKeys = attachment.getRecipientPublicKeys();
            if (recipientPublicKeys.length != shuffling.getParticipantCount() && recipientPublicKeys.length != 0) {
                throw new AplException.NotValidException(String.format("Invalid number of recipient public keys %d", recipientPublicKeys.length));
            }
            Set<Long> recipientAccounts = new HashSet<>(recipientPublicKeys.length);
            for (byte[] recipientPublicKey : recipientPublicKeys) {
                if (!Crypto.isCanonicalPublicKey(recipientPublicKey)) {
                    throw new AplException.NotValidException("Invalid recipient public key " + Convert.toHexString(recipientPublicKey));
                }
                if (!recipientAccounts.add(AccountService.getId(recipientPublicKey))) {
                    throw new AplException.NotValidException("Duplicate recipient accounts");
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            ShufflingRecipientsAttachment attachment = (ShufflingRecipientsAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            return TransactionType.isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            ShufflingRecipientsAttachment attachment = (ShufflingRecipientsAttachment) transaction.getAttachment();
            Shuffling shuffling = lookupShufflingService().getShuffling(attachment.getShufflingId());
            lookupShufflingService().updateRecipients(shuffling, transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isPhasable() {
            return false;
        }

    };

    private ShufflingTransaction() {
    }

    public static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_SHUFFLING_CREATION:
                return SHUFFLING_CREATION;
            case SUBTYPE_SHUFFLING_REGISTRATION:
                return SHUFFLING_REGISTRATION;
            case SUBTYPE_SHUFFLING_PROCESSING:
                return SHUFFLING_PROCESSING;
            case SUBTYPE_SHUFFLING_RECIPIENTS:
                return SHUFFLING_RECIPIENTS;
            case SUBTYPE_SHUFFLING_VERIFICATION:
                return SHUFFLING_VERIFICATION;
            case SUBTYPE_SHUFFLING_CANCELLATION:
                return SHUFFLING_CANCELLATION;
            default:
                return null;
        }
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_SHUFFLING;
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

}
