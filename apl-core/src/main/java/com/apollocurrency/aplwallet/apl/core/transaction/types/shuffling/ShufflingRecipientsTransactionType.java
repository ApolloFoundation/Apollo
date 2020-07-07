/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_PROCESSING;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_RECIPIENTS;
@Singleton
public class ShufflingRecipientsTransactionType extends ShufflingTransactionType {
    private final static Fee SHUFFLING_RECIPIENTS_FEE = new Fee.ConstantFee(11 * Constants.ONE_APL);

    @Inject
    public ShufflingRecipientsTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return SHUFFLING_RECIPIENTS;
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
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        if (shuffling == null) {
            throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
            throw new AplException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                Long.toUnsignedString(attachment.getShufflingId())));
        }
        ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
        if (participant == null) {
            throw new AplException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
        }
        if (participant.getNextAccountId() != 0) {
            throw new AplException.NotValidException(String.format("Participant %s is not last in shuffle",
                Long.toUnsignedString(transaction.getSenderId())));
        }
        if (!participant.getState().canBecome(ShufflingParticipant.State.PROCESSED)) {
            throw new AplException.NotCurrentlyValidException(String.format("Participant %s processing already complete",
                Long.toUnsignedString(transaction.getSenderId())));
        }
        if (participant.getAccountId() != shuffling.getAssigneeAccountId()) {
            throw new AplException.NotCurrentlyValidException(String.format("Participant %s is not currently assigned to process shuffling %s",
                Long.toUnsignedString(participant.getAccountId()), Long.toUnsignedString(shuffling.getId())));
        }
        byte[] shufflingStateHash = shuffling.getStateHash();
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
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingRecipientsAttachment attachment = (ShufflingRecipientsAttachment) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        return TransactionType.isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ShufflingRecipientsAttachment attachment = (ShufflingRecipientsAttachment) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        shuffling.updateRecipients(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

}
