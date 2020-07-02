/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.control;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class SetPhasingOnlyTransactionType extends AccountControlTransactionType {
    private final AccountControlPhasingService accountControlPhasingService;
    @Inject
    public SetPhasingOnlyTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountControlPhasingService accountControlPhasingService) {
        super(blockchainConfig, accountService);
        this.accountControlPhasingService = accountControlPhasingService;
    }
    @Override
    public byte getSubtype() {
        return TransactionTypes.SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY;
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
            Account senderAccount = getAccountService().getAccount(transaction.getSenderId());
            if (senderAccount == null || !senderAccount.getControls().contains(AccountControlType.PHASING_ONLY)) {
                throw new AplException.NotCurrentlyValidException("Phasing only account control is not currently enabled");
            }
        } else if (votingModel == VoteWeighting.VotingModel.TRANSACTION || votingModel == VoteWeighting.VotingModel.HASH) {
            throw new AplException.NotValidException("Invalid voting model " + votingModel + " for account control");
        }
        long maxFees = attachment.getMaxFees();
        long maxFeesLimit = (attachment.getPhasingParams().getVoteWeighting().isBalanceIndependent() ? 3 : 22) * Constants.ONE_APL;
        BlockchainConfig blockchainConfig = getBlockchainConfig();
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
        return TransactionType.isDuplicate(this, Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        SetPhasingOnly attachment = (SetPhasingOnly) transaction.getAttachment();
        accountControlPhasingService.set(senderAccount, attachment);
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
}
