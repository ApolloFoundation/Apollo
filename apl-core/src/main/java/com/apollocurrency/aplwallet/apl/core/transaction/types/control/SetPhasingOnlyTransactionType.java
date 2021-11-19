/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.control;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class SetPhasingOnlyTransactionType extends AccountControlTransactionType {
    private final AccountControlPhasingService accountControlPhasingService;
    private final PhasingPollService phasingPollService;
    @Inject
    public SetPhasingOnlyTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountControlPhasingService accountControlPhasingService, PhasingPollService phasingPollService) {
        super(blockchainConfig, accountService);
        this.accountControlPhasingService = accountControlPhasingService;
        this.phasingPollService = phasingPollService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SET_PHASING_ONLY;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        SetPhasingOnly attachment = (SetPhasingOnly) transaction.getAttachment();
        VoteWeighting.VotingModel votingModel = attachment.getPhasingParams().getVoteWeighting().getVotingModel();
        phasingPollService.validateStateDependent(attachment.getPhasingParams());
        if (votingModel == VoteWeighting.VotingModel.NONE) {
            Account senderAccount = getAccountService().getAccount(transaction.getSenderId());
            if (senderAccount == null || !senderAccount.getControls().contains(AccountControlType.PHASING_ONLY)) {
                throw new AplException.NotCurrentlyValidException("Phasing only account control is not currently enabled");
            }
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        SetPhasingOnly attachment = (SetPhasingOnly) transaction.getAttachment();
        VoteWeighting.VotingModel votingModel = attachment.getPhasingParams().getVoteWeighting().getVotingModel();
        phasingPollService.validateStateIndependent(attachment.getPhasingParams());
        if (votingModel == VoteWeighting.VotingModel.TRANSACTION || votingModel == VoteWeighting.VotingModel.HASH) {
            throw new AplException.NotValidException("Invalid voting model " + votingModel + " for account control");
        }
        long maxFees = attachment.getMaxFees();
        BlockchainConfig blockchainConfig = getBlockchainConfig();
        long maxFeesLimit = Math.multiplyExact((attachment.getPhasingParams().getVoteWeighting().isBalanceIndependent() ? 3 : 22), blockchainConfig.getOneAPL());

        if (maxFees < 0 || (maxFees > 0 && maxFees < maxFeesLimit) || maxFees > blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException(String.format("Invalid max fees %f %s", ((double) maxFees) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol()));
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
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return TransactionType.isDuplicate(getSpec(), Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
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
