/*
 * Copyright (c) 2021-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactoryCreator;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.smc.contract.ContractException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;
import com.apollocurrency.smc.polyglot.PolyglotException;
import com.apollocurrency.smc.polyglot.engine.InternalNotRecoverableException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcTransactionType extends TransactionType {
    protected SmcContractRepository contractRepository;
    protected ContractToolService contractToolService;
    protected final SmcFuelValidator fuelMinMaxValidator;
    protected final SmcBlockchainIntegratorFactoryCreator integratorFactoryCreator;
    protected final Blockchain blockchain;
    protected final SmcConfig smcConfig;

    AbstractSmcTransactionType(BlockchainConfig blockchainConfig, Blockchain blockchain,
                               AccountService accountService,
                               SmcContractRepository contractRepository,
                               ContractToolService contractToolService,
                               SmcFuelValidator fuelMinMaxValidator,
                               SmcBlockchainIntegratorFactoryCreator integratorFactoryCreator,
                               SmcConfig smcConfig) {
        super(blockchainConfig, accountService);
        this.contractRepository = contractRepository;
        this.contractToolService = contractToolService;
        this.fuelMinMaxValidator = fuelMinMaxValidator;
        this.integratorFactoryCreator = integratorFactoryCreator;
        this.blockchain = blockchain;
        this.smcConfig = smcConfig;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        //TODO calculate the required fuel value by executing the contract
        //currently use: fee = fuelPrice * fuelLimit
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        var fee = attachment.getFuelPrice().multiply(attachment.getFuelLimit()).longValueExact();
        return new Fee.ConstantFee(fee);
    }

    @Override
    public boolean canFailDuringExecution() {
        return true;
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
    public final void doStateIndependentValidation(Transaction transaction) throws AplUnacceptableTransactionValidationException {
        checkPrecondition(transaction);
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        fuelMinMaxValidator.validate(transaction);
        executeStateIndependentValidation(transaction, attachment);
    }

    public abstract void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) throws AplUnacceptableTransactionValidationException;

    private void checkPrecondition(Transaction smcTransaction) {
        if (smcTransaction.getVersion() < 2) {
            log.error("Inconsistent transaction fields, the '{}' transaction type doesn't match the transaction version.", getSpec().getCompatibleName());
            throw new AplUnacceptableTransactionValidationException("Unsupported transaction version: " + smcTransaction.getVersion(), smcTransaction);
        }
        if (!getBlockchainConfig().isSmcTransactionsActiveAtHeight(blockchain.getHeight())) {
            throw new AplTransactionFeatureNotEnabledException("'Smart-contract transactions'", smcTransaction);
        }
    }
/*
    DON'T REMOVE
    protected void refundRemaining(Transaction transaction, Account senderAccount, Fuel fuel) {
        if (fuel.hasRemaining()) {
            //refund remaining fuel
            log.debug("fuel={}, refunded fee={}, account={}", fuel, fuel.refundedFee().longValueExact(), senderAccount.getId());
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), fuel.refundedFee().longValueExact());
        }
    }
*/

    protected void executeContract(Transaction transaction, Account senderAccount, SmartContract smartContract, SmcContractTxProcessor processor) {
        try {
            processor.process();
        } catch (InternalNotRecoverableException e) {
            log.error(processor.getExecutionLog().toJsonString());
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n", e);
            e.printStackTrace();
            System.exit(1);
        } catch (ContractException e) {
            var cause = e.getCause();
            String message;
            if (cause == null) {
                log.error(e.getClass().getName() + ": " + processor.getExecutionLog().toJsonString());
                message = e.getClass().getName();
            } else {
                //cause instanceof JSRevertException or JSRequirementException or JSAssertionException or JSException
                Fuel fuel = smartContract.getFuel();
                message = e.getMessage();
                log.info("{}:{} Contract={} Fuel={}", e.getClass().getSimpleName(), message, smartContract.address(), fuel);
            }
            throw new AplTransactionExecutionException(message, e, transaction);
        } catch (PolyglotException e) {
            log.error(e.getClass().getName() + ": " + processor.getExecutionLog().toJsonString());
            throw new AplTransactionExecutionException(e.getClass().getName(), e, transaction);
        }
        log.debug("Commit the contract state changes...");
        processor.commit();
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
