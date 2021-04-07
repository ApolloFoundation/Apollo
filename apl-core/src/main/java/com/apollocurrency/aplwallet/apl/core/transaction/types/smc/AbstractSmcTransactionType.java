/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.AplBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.contract.fuel.Fuel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcTransactionType extends TransactionType {
    protected ContractService contractService;
    protected final AplBlockchainIntegratorFactory integratorFactory;

    public AbstractSmcTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, ContractService contractService, AplBlockchainIntegratorFactory integratorFactory) {
        super(blockchainConfig, accountService);
        this.contractService = contractService;
        this.integratorFactory = integratorFactory;
    }

    @Override
    public String getName() {
        return getSpec().getCompatibleName();
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), Long.toUnsignedString(transaction.getId()), duplicates, true);
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    protected void checkPrecondition(Transaction smcTransaction) {
        smcTransaction.getAttachment().getTransactionTypeSpec();
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != getSpec()) {
            log.error("Invalid transaction attachment, txType={} txId={}", smcTransaction.getType(), smcTransaction.getId());
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec());
        }
    }

    protected void refundRemaining(Transaction transaction, Account senderAccount, Fuel fuel) {
        if (fuel.hasRemaining()) {
            //refund remaining fuel
            log.debug("refunded fee={}, account={}", fuel.refundedFee().longValueExact(), senderAccount.getId());
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, null, transaction.getId(), 0, fuel.refundedFee().longValueExact());
            getAccountService().logAccountLedger(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), fuel.refundedFee().longValueExact());
        }
    }

}
