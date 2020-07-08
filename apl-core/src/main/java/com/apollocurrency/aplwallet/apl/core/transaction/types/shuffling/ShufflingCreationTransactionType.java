/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class ShufflingCreationTransactionType extends ShufflingTransactionType {
    private final AssetService assetService;

    @Inject
    public ShufflingCreationTransactionType(AssetService assetService, BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_CREATION;
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
            BlockchainConfig blockchainConfig = getBlockchainConfig();
            if (amount < blockchainConfig.getShufflingDepositAtm() || amount > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid ATM amount " + amount
                    + ", minimum is " + blockchainConfig.getShufflingDepositAtm());
            }
        } else if (holdingType == HoldingType.ASSET) {
            Asset asset = assetService.getAsset(attachment.getHoldingId());
            if (asset == null) {
                throw new AplException.NotCurrentlyValidException("Unknown asset " + Long.toUnsignedString(attachment.getHoldingId()));
            }
            if (amount <= 0 || amount > asset.getInitialQuantityATU()) {
                throw new AplException.NotValidException("Invalid asset quantity " + amount);
            }
        } else if (holdingType == HoldingType.CURRENCY) {
            Currency currency = Currency.getCurrency(attachment.getHoldingId());
            CurrencyType.validate(currency, transaction);
            if (!currency.isActive()) {
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
            BlockchainConfig blockchainConfig = getBlockchainConfig();
            if (holdingType.getUnconfirmedBalance(senderAccount, attachment.getHoldingId()) >= attachment.getAmount()
                && senderAccount.getUnconfirmedBalanceATM() >= blockchainConfig.getShufflingDepositAtm()) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), -attachment.getAmount());
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -blockchainConfig.getShufflingDepositAtm());
                return true;
            }
        } else {
            if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getAmount()) {
                getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -attachment.getAmount());
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
        Shuffling.addShuffling(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
        HoldingType holdingType = attachment.getHoldingType();
        if (holdingType != HoldingType.APL) {
            holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), attachment.getAmount());
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), getBlockchainConfig().getShufflingDepositAtm());
        } else {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAmount());
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ShufflingCreation attachment = (ShufflingCreation) transaction.getAttachment();
        if (attachment.getHoldingType() != HoldingType.CURRENCY) {
            return false;
        }
        Currency currency = Currency.getCurrency(attachment.getHoldingId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.CURRENCY_ISSUANCE, nameLower, duplicates, false);
        if (!nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.CURRENCY_ISSUANCE, codeLower, duplicates, false);
        }
        return isDuplicate;
    }

}
