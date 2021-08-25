/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_PROCESSING;

@Singleton
public class ShufflingProcessingTransactionType extends ShufflingTransactionType {
    public static final String NAME = "ShufflingProcessing";
    private final Blockchain blockchain;
    private final TimeService timeService;
    private final ShufflingService shufflingService;

    @Inject
    public ShufflingProcessingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, Blockchain blockchain, TimeService timeService, ShufflingService shufflingService) {
        super(blockchainConfig, accountService);
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.shufflingService = shufflingService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return SHUFFLING_PROCESSING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SHUFFLING_PROCESSING;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return getFeeFactory().createFixed(BigDecimal.TEN);
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        if (shuffling == null) {
            throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        if (shuffling.getStage() != ShufflingStage.PROCESSING) {
            throw new AplException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                Long.toUnsignedString(attachment.getShufflingId())));
        }
        ShufflingParticipant participant = shufflingService.getParticipant(shuffling.getId(), transaction.getSenderId());
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
        byte[] shufflingStateHash = shufflingService.getStageHash(shuffling);
        if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
            throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
        }
        byte[][] data = attachment.getData();
        if (data != null) {
            if (data.length != participant.getIndex() + 1 && data.length != 0) {
                throw new AplException.NotValidException(String.format("Invalid number of encrypted data %d for participant number %d",
                    data.length, participant.getIndex()));
            }
            for (byte[] bytes : data) {
                if (bytes.length != 32 + 64 * (shuffling.getParticipantCount() - participant.getIndex() - 1)) {
                    throw new AplException.NotValidException("Invalid encrypted data length " + bytes.length);
                }
            }
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
        byte[][] data = attachment.getData();
        if (data == null && timeService.getEpochTime() - transaction.getTimestamp() < getBlockchainConfig().getMinPrunableLifetime()) {
            throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
        }
        if (data != null) {
            byte[] previous = null;
            for (byte[] bytes : data) {
                if (previous != null && Convert.byteArrayComparator.compare(previous, bytes) >= 0) {
                    throw new AplException.NotValidException("Duplicate or unsorted encrypted data");
                }
                previous = bytes;
            }
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        return TransactionType.isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        shufflingService.updateParticipantData(shuffling, transaction, attachment);
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
        Transaction transaction = blockchain.getTransaction(transactionId);

        ShufflingProcessingAttachment attachment = (ShufflingProcessingAttachment) transaction.getAttachment();
        return shufflingService.getData(attachment.getShufflingId(), transaction.getSenderId()) == null;
    }

}
