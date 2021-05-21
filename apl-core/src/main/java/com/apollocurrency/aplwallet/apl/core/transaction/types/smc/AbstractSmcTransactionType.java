/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;
import com.apollocurrency.smc.contract.fuel.FuelValidator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcTransactionType extends TransactionType {
    protected SmcContractService contractService;
    protected final FuelValidator fuelValidator;
    protected final SmcBlockchainIntegratorFactory integratorFactory;

    AbstractSmcTransactionType(BlockchainConfig blockchainConfig, AccountService accountService,
                               SmcContractService contractService,
                               FuelValidator fuelValidator,
                               SmcBlockchainIntegratorFactory integratorFactory) {
        super(blockchainConfig, accountService);
        this.contractService = contractService;
        this.fuelValidator = fuelValidator;
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

    @Override
    public final void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        checkPrecondition(transaction);
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        if (!fuelValidator.validateLimitValue(attachment.getFuelLimit())) {
            throw new AplException.NotCurrentlyValidException("Fuel limit value doesn't correspond to the MIN or MAX values.");
        }
        if (!fuelValidator.validatePriceValue(attachment.getFuelPrice())) {
            throw new AplException.NotCurrentlyValidException("Fuel price value doesn't correspond to the MIN or MAX values.");
        }

        executeStateIndependentValidation(transaction, attachment);
    }

    public abstract void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) throws AplException.ValidationException;

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
            log.debug("fuel={}, refunded fee={}, account={}", fuel, fuel.refundedFee().longValueExact(), senderAccount.getId());
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), fuel.refundedFee().longValueExact());
        }
    }

    static class SmcFuelBasedFee extends Fee.FuelBasedFee {
        public SmcFuelBasedFee(FuelCalculator fuelCalculator) {
            super(fuelCalculator);
        }

        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            return ((AbstractSmcAttachment) transaction.getAttachment()).getPayableSize();
        }

        @Override
        public BigInteger getFuelPrice(Transaction transaction, Appendix appendage) {
            return ((AbstractSmcAttachment) appendage).getFuelPrice();
        }
    }

}
