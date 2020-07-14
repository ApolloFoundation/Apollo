/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AccountControlEffectiveBalanceLeasing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author al
 */
public abstract class AccountControl extends TransactionType {

    public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        }

        @Override
        public String getName() {
            return "EffectiveBalanceLeasing";
        }

        @Override
        public AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new AccountControlEffectiveBalanceLeasing(buffer);
        }

        @Override
        public AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new AccountControlEffectiveBalanceLeasing(attachmentData);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            AccountControlEffectiveBalanceLeasing attachment = (AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
            Account sender = lookupAccountService().getAccount(transaction.getSenderId());
            lookupAccountLeaseService().leaseEffectiveBalance(sender, transaction.getRecipientId(), attachment.getPeriod());
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            AccountControlEffectiveBalanceLeasing attachment = (AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
            if (transaction.getSenderId() == transaction.getRecipientId()) {
                throw new AplException.NotValidException("Account cannot lease balance to itself");
            }
            if (transaction.getAmountATM() != 0) {
                throw new AplException.NotValidException("Transaction amount must be 0 for effective balance leasing");
            }
            if (attachment.getPeriod() < lookupBlockchainConfig().getLeasingDelay() || attachment.getPeriod() > 65535) {
                throw new AplException.NotValidException("Invalid effective balance leasing period: " + attachment.getPeriod());
            }
            byte[] recipientPublicKey = lookupAccountService().getPublicKeyByteArray(transaction.getRecipientId());
            if (recipientPublicKey == null) {
                throw new AplException.NotCurrentlyValidException("Invalid effective balance leasing: " + " recipient account " + Long.toUnsignedString(transaction.getRecipientId()) + " not found or no public key published");
            }
            if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
                throw new AplException.NotValidException("Leasing to Genesis account not allowed");
            }
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
    public static final TransactionType SET_PHASING_ONLY = new AccountControl() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ACCOUNT_CONTROL_PHASING_ONLY;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return new SetPhasingOnly(buffer);
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new SetPhasingOnly(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            SetPhasingOnly attachment = (SetPhasingOnly) transaction.getAttachment();
            VoteWeighting.VotingModel votingModel = attachment.getPhasingParams().getVoteWeighting().getVotingModel();
            attachment.getPhasingParams().validate();
            if (votingModel == VoteWeighting.VotingModel.NONE) {
                Account senderAccount = lookupAccountService().getAccount(transaction.getSenderId());
                if (senderAccount == null || !senderAccount.getControls().contains(AccountControlType.PHASING_ONLY)) {
                    throw new AplException.NotCurrentlyValidException("Phasing only account control is not currently enabled");
                }
            } else if (votingModel == VoteWeighting.VotingModel.TRANSACTION || votingModel == VoteWeighting.VotingModel.HASH) {
                throw new AplException.NotValidException("Invalid voting model " + votingModel + " for account control");
            }
            long maxFees = attachment.getMaxFees();
            long maxFeesLimit = (attachment.getPhasingParams().getVoteWeighting().isBalanceIndependent() ? 3 : 22) * Constants.ONE_APL;
            BlockchainConfig blockchainConfig = lookupBlockchainConfig();
            if (maxFees < 0 || (maxFees > 0 && maxFees < maxFeesLimit) || maxFees > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException(String.format("Invalid max fees %f %s", ((double) maxFees) / Constants.ONE_APL, blockchainConfig.getCoinSymbol()));
            }
            short minDuration = attachment.getMinDuration();
            if (minDuration < 0 || (minDuration > 0 && minDuration < 3) || minDuration >= Constants.MAX_PHASING_DURATION) {
                throw new AplException.NotValidException("Invalid min duration " + attachment.getMinDuration());
            }
            short maxDuration = attachment.getMaxDuration();
            if (maxDuration < 0 || (maxDuration > 0 && maxDuration < 3) || maxDuration >= Constants.MAX_PHASING_DURATION) {
                throw new AplException.NotValidException("Invalid max duration " + maxDuration);
            }
            if (minDuration > maxDuration) {
                throw new AplException.NotValidException(String.format("Min duration %d cannot exceed max duration %d ", minDuration, maxDuration));
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            return TransactionType.isDuplicate(SET_PHASING_ONLY, Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            SetPhasingOnly attachment = (SetPhasingOnly) transaction.getAttachment();
//            PhasingOnly.set(senderAccount, attachment);
            lookupAccountControlPhasingService().set(senderAccount, attachment);
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public String getName() {
            return "SetPhasingOnly";
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }
    };

    private AccountControl() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_ACCOUNT_CONTROL;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

}
