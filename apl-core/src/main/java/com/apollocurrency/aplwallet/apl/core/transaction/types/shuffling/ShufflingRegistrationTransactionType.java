/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_REGISTRATION;
@Singleton
public class ShufflingRegistrationTransactionType extends ShufflingTransactionType {
    private final Blockchain blockchain;

    @Inject
    public ShufflingRegistrationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, Blockchain blockchain) {
        super(blockchainConfig, accountService);
        this.blockchain = blockchain;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return null;
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
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        if (shuffling == null) {
            throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        byte[] shufflingStateHash = shuffling.getStateHash();
        if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
            throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
        }
        if (shuffling.getStage() != Shuffling.Stage.REGISTRATION) {
            throw new AplException.NotCurrentlyValidException("Shuffling registration has ended for " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        if (shuffling.getParticipant(transaction.getSenderId()) != null) {
            throw new AplException.NotCurrentlyValidException(String.format("Account %s is already registered for shuffling %s",
                Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
        }
        if (blockchain.getHeight() + shuffling.getBlocksRemaining() <= attachment.getFinishValidationHeight(transaction)) {
            throw new AplException.NotCurrentlyValidException("Shuffling registration finishes in " + shuffling.getBlocksRemaining() + " blocks");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        return TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
            Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true)
            || TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
            Long.toUnsignedString(shuffling.getId()), duplicates, shuffling.getParticipantCount() - shuffling.getRegistrantCount());
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        HoldingType holdingType = shuffling.getHoldingType();
        if (holdingType != HoldingType.APL) {
            BlockchainConfig blockchainConfig = getBlockchainConfig();
            if (holdingType.getUnconfirmedBalance(senderAccount, shuffling.getHoldingId()) >= shuffling.getAmount()
                && senderAccount.getUnconfirmedBalanceATM() >= blockchainConfig.getShufflingDepositAtm()) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), -shuffling.getAmount());
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -blockchainConfig.getShufflingDepositAtm());
                return true;
            }
        } else {
            if (senderAccount.getUnconfirmedBalanceATM() >= shuffling.getAmount()) {
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -shuffling.getAmount());
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        shuffling.addParticipant(transaction.getSenderId());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
        HoldingType holdingType = shuffling.getHoldingType();
        if (holdingType != HoldingType.APL) {
            holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), shuffling.getAmount());
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), getBlockchainConfig().getShufflingDepositAtm());
        } else {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getAmount());
        }
    }

}
