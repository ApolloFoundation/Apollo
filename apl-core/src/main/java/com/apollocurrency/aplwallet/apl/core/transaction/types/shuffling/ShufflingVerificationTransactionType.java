/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingVerificationAttachment;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_VERIFICATION;
@Singleton
public class ShufflingVerificationTransactionType extends ShufflingTransactionType {

    @Inject
    public ShufflingVerificationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return SHUFFLING_VERIFICATION;
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
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        if (shuffling == null) {
            throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        if (shuffling.getStage() != Shuffling.Stage.VERIFICATION) {
            throw new AplException.NotCurrentlyValidException("Shuffling not in verification stage: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
        if (participant == null) {
            throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
        }
        if (!participant.getState().canBecome(ShufflingParticipant.State.VERIFIED)) {
            throw new AplException.NotCurrentlyValidException(String.format("Shuffling participant %s in state %s cannot become verified",
                Long.toUnsignedString(attachment.getShufflingId()), participant.getState()));
        }
        if (participant.getIndex() == shuffling.getParticipantCount() - 1) {
            throw new AplException.NotValidException("Last participant cannot submit verification transaction");
        }
        byte[] shufflingStateHash = shuffling.getStateHash();
        if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
            throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingVerificationAttachment attachment = (ShufflingVerificationAttachment) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
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
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        shuffling.verify(transaction.getSenderId());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

}
