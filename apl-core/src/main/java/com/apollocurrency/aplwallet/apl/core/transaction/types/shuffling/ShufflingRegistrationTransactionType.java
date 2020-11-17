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
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
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
    private final ShufflingService shufflingService;
    private final TransactionValidator validator;

    @Inject
    public ShufflingRegistrationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, Blockchain blockchain, ShufflingService shufflingService, TransactionValidator validator) {
        super(blockchainConfig, accountService);
        this.blockchain = blockchain;
        this.shufflingService = shufflingService;
        this.validator = validator;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return SHUFFLING_REGISTRATION;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        if (shuffling == null) {
            throw new AplException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        byte[] shufflingStateHash = shufflingService.getStageHash(shuffling);
        if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
            throw new AplException.NotCurrentlyValidException("Shuffling state hash doesn't match");
        }
        if (shuffling.getStage() != ShufflingStage.REGISTRATION) {
            throw new AplException.NotCurrentlyValidException("Shuffling registration has ended for " + Long.toUnsignedString(attachment.getShufflingId()));
        }
        if (shufflingService.getParticipant(shuffling.getId(), transaction.getSenderId()) != null) {
            throw new AplException.NotCurrentlyValidException(String.format("Account %s is already registered for shuffling %s",
                Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
        }
        if (blockchain.getHeight() + shuffling.getBlocksRemaining() <= validator.getFinishValidationHeight(transaction, attachment)) {
            throw new AplException.NotCurrentlyValidException("Shuffling registration finishes in " + shuffling.getBlocksRemaining() + " blocks");
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        return TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
            Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true)
            || TransactionType.isDuplicate(SHUFFLING_REGISTRATION,
            Long.toUnsignedString(shuffling.getId()), duplicates, shuffling.getParticipantCount() - shuffling.getRegistrantCount());
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
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
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        shufflingService.addParticipant(shuffling, transaction.getSenderId());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ShufflingRegistration attachment = (ShufflingRegistration) transaction.getAttachment();
        Shuffling shuffling = shufflingService.getShuffling(attachment.getShufflingId());
        HoldingType holdingType = shuffling.getHoldingType();
        if (holdingType != HoldingType.APL) {
            holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), shuffling.getAmount());
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), getBlockchainConfig().getShufflingDepositAtm());
        } else {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getAmount());
        }
    }

}
