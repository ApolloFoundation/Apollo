/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountControlPhasingTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SetPhasingOnly;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SET_PHASING_ONLY;

@Slf4j
@Singleton
public class AccountControlPhasingServiceImpl implements AccountControlPhasingService {

    private final AccountControlPhasingTable accountControlPhasingTable;
    private final AccountService accountService;
    private final BlockChainInfoService blockChainInfoService;
    private final PhasingPollService phasingPollService;
    private final BlockchainConfig blockchainConfig;
    private final IteratorToStreamConverter<AccountControlPhasing> accountControlPhasingIteratorToStreamConverter =
        new IteratorToStreamConverter<>();

    @Inject
    public AccountControlPhasingServiceImpl(AccountControlPhasingTable accountControlPhasingTable,
                                            AccountService accountService,
                                            BlockChainInfoService blockChainInfoService,
                                            PhasingPollService phasingPollService,
                                            BlockchainConfig blockchainConfig) {
        this.accountControlPhasingTable = accountControlPhasingTable;
        this.accountService = accountService;
        this.blockChainInfoService = blockChainInfoService;
        this.phasingPollService = phasingPollService;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public AccountControlPhasing get(long accountId) {
        return accountControlPhasingTable.getBy(
            new DbClause.LongClause("account_id", accountId).and(
                new DbClause.ByteClause("voting_model", DbClause.Op.NE, VoteWeighting.VotingModel.NONE.getCode()))
        );
    }

    @Override
    public int getCount() {
        return accountControlPhasingTable.getCount();
    }

    @Override
    public DbIterator<AccountControlPhasing> getAll(int from, int to) {
        return accountControlPhasingTable.getAll(from, to);
    }

    @Override
    public Stream<AccountControlPhasing> getAllStream(int from, int to) {
        return accountControlPhasingIteratorToStreamConverter.apply(accountControlPhasingTable.getAll(from, to));
    }

    @Override
    public void unset(Account account) {
        account.removeControl(AccountControlType.PHASING_ONLY);
        AccountControlPhasing phasingOnly = this.get(account.getId());
        accountControlPhasingTable.deleteAtHeight(phasingOnly, blockChainInfoService.getHeight());
    }

    @Override
    public void set(Account senderAccount, SetPhasingOnly attachment) {
        PhasingParams phasingParams = attachment.getPhasingParams();
        if (phasingParams.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.NONE) {
            //no voting - remove the control
            senderAccount.removeControl(AccountControlType.PHASING_ONLY);
            AccountControlPhasing phasingOnly = this.get(senderAccount.getId());
            phasingOnly.setPhasingParams(phasingParams);
            phasingOnly.setHeight(blockChainInfoService.getHeight());// height should be updated for shard's hash match
            accountControlPhasingTable.deleteAtHeight(phasingOnly, blockChainInfoService.getHeight());
            unset(senderAccount);
        } else {
            senderAccount.addControl(AccountControlType.PHASING_ONLY);
            AccountControlPhasing phasingOnly = this.get(senderAccount.getId());
            if (phasingOnly == null) {
                phasingOnly = new AccountControlPhasing(
                    AccountControlPhasingTable.accountControlPhasingDbKeyFactory.newKey(senderAccount.getId()),
                    senderAccount.getId(), phasingParams,
                    attachment.getMaxFees(), attachment.getMinDuration(), attachment.getMaxDuration(),
                    blockChainInfoService.getHeight());
            } else {
                phasingOnly.setPhasingParams(phasingParams);
                phasingOnly.setMaxFees(attachment.getMaxFees());
                phasingOnly.setMinDuration(attachment.getMinDuration());
                phasingOnly.setMaxDuration(attachment.getMaxDuration());
                phasingOnly.setHeight(blockChainInfoService.getHeight());
            }
            accountControlPhasingTable.insert(phasingOnly);
        }
        accountService.update(senderAccount);
    }

    @Override
    public void checkTransaction(Transaction transaction) throws AplException.NotCurrentlyValidException {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        if (senderAccount == null) {
            throw new AplException.NotCurrentlyValidException("Account " + transaction.getSenderId() + " does not exist yet");
        }
        if (senderAccount.getControls().contains(AccountControlType.PHASING_ONLY)) {
            AccountControlPhasing phasingOnly = this.get(transaction.getSenderId());
            this.checkTransactionByPhasing(transaction, phasingOnly);
        }
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, Set<AccountControlType> senderAccountControls, AccountControlPhasing accountControlPhasing) {
        return
            senderAccountControls.contains(AccountControlType.PHASING_ONLY)
                && (accountControlPhasing != null && accountControlPhasing.getMaxFees() != 0)
                && transaction.getType().getSpec() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }

    private void checkTransactionByPhasing(Transaction transaction, AccountControlPhasing phasingOnly) throws AplException.AccountControlException {
        if (phasingOnly.getMaxFees() > 0 && Math.addExact(transaction.getFeeATM(),
            phasingPollService.getSenderPhasedTransactionFees(transaction.getSenderId())) > phasingOnly.getMaxFees()) {
            throw new AplException.AccountControlException(
                String.format("Maximum total fees limit of %f %s exceeded",
                    ((double) phasingOnly.getMaxFees()) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol()));
        }
        if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PHASING_VOTE_CASTING) {
            return;
        }
        try {
            phasingPollService.checkApprovable(phasingOnly.getPhasingParams());
        } catch (AplException.NotCurrentlyValidException e) {
            //LOG.debug("Account control no longer valid: " + e.getMessage());
            return;
        }
        PhasingAppendix phasingAppendix = transaction.getPhasing();
        if (phasingAppendix == null) {
            throw new AplException.AccountControlException("Non-phased transaction when phasing account control is enabled");
        }
        if (!phasingOnly.getPhasingParams().equals(phasingAppendix.getParams())) {
            throw new AplException.AccountControlException(
                "Phasing parameters mismatch phasing account control. Expected: " + phasingOnly.getPhasingParams().toString()
                    + " . Actual: " + phasingAppendix.getParams().toString());
        }
        int duration = phasingAppendix.getFinishHeight() - blockChainInfoService.getHeight();
        if ((phasingOnly.getMaxDuration() > 0 && duration > phasingOnly.getMaxDuration())
            || (phasingOnly.getMinDuration() > 0 && duration < phasingOnly.getMinDuration())) {
            throw new AplException.AccountControlException("Invalid phasing duration " + duration);
        }
    }

    /**
     * @deprecated see method with longer parameters list below
     */
    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        Set<AccountControlType> senderAccountControls = senderAccount.getControls();
        AccountControlPhasing accountControlPhasing = this.get(transaction.getSenderId());
        return
            senderAccountControls.contains(AccountControlType.PHASING_ONLY)
                && (accountControlPhasing != null && accountControlPhasing.getMaxFees() != 0)
                && transaction.getType().getSpec() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
    }
}