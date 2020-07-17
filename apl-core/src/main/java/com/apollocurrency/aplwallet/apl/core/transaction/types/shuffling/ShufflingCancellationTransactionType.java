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
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_CANCELLATION;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_VERIFICATION;
@Singleton
class ShufflingCancellationTransactionType extends ShufflingTransactionType {
    private final Blockchain blockchain;
    private final ShufflingService shufflingService;

    @Inject
    public ShufflingCancellationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, Blockchain blockchain, ShufflingService shufflingService) {
        super(blockchainConfig, accountService);
        this.blockchain = blockchain;
        this.shufflingService = shufflingService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return SHUFFLING_CANCELLATION;
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
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
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
        ShufflingParticipant participant = shufflingService.getParticipant(shuffling.getId(), transaction.getSenderId());
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
        byte[] shufflingStateHash = shufflingService.getStageHash(shuffling);
        if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
            throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
        }

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
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingCancellationAttachment attachment = (ShufflingCancellationAttachment) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
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
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        ShufflingParticipant participant = shufflingService.getParticipant(shuffling.getId(), senderAccount.getId());
        shufflingService.cancelBy(shuffling, participant, attachment.getBlameData(), attachment.getKeySeeds());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isPhasable() {
        return false;
    }
}
