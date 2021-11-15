/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.PostponedContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallMethodTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallMethodTxValidator;
import com.apollocurrency.aplwallet.apl.smc.service.tx.SyntaxValidator;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.FuelValidator;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.data.type.Address;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcCallMethodTransactionType extends AbstractSmcTransactionType {
    @Inject
    public SmcCallMethodTransactionType(BlockchainConfig blockchainConfig, AccountService accountService,
                                        PostponedContractService contractService,
                                        ContractToolService contractToolService,
                                        FuelValidator fuelValidator,
                                        SmcBlockchainIntegratorFactory integratorFactory,
                                        SmcConfig smcConfig) {
        super(blockchainConfig, accountService, contractService, contractToolService, fuelValidator, integratorFactory, smcConfig);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SMC_CALL_METHOD;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new SmcCallMethodAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(RlpReader reader) {
        return new SmcCallMethodAttachment(reader);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) {
        return new SmcCallMethodAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        //there is only one contract execution in the same block, contract address == transaction.getRecipientId()
        return isDuplicate(getSpec(), Long.toUnsignedString(transaction.getRecipientId()), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) {
        log.debug("SMC: doStateDependentValidation = ...");
        Address address = new AplAddress(transaction.getRecipientId());
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        SmartContract smartContract;
        try {
            smartContract = contractService.loadContract(
                address,
                transactionSender,
                transactionSender,
                new ContractFuel(address, attachment.getFuelLimit(), attachment.getFuelPrice())
            );
        } catch (AddressNotFoundException e) {
            throw new AplAcceptableTransactionValidationException("Contract doesn't exist at address " + address, transaction);
        }
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        var context = SmcConfig.asContext(integratorFactory.createMockProcessor(transaction.getId()));
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new CallMethodTxValidator(
            smartContract,
            smartMethod,
            context
        );
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("SMC: doStateDependentValidation = INVALID");
            throw new AplAcceptableTransactionValidationException(executionLog.toJsonString(), transaction);
        }
        log.debug("SMC: doStateDependentValidation = VALID");
    }

    @Override
    public void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) {
        log.debug("SMC: doStateIndependentValidation = ...");
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) abstractSmcAttachment;
        if (Strings.isNullOrEmpty(attachment.getMethodName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract method name.", transaction);
        }
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        BigInteger calculatedFuel = getFuelBasedFee(transaction).calcFuel(smartMethod);
        Fuel actualFuel = new ContractFuel(new AplAddress(transaction.getRecipientId()), attachment.getFuelLimit(), attachment.getFuelPrice());
        if (!actualFuel.tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, actualFuel);
            throw new AplUnacceptableTransactionValidationException("Not enough fuel to execute this transaction, expected="
                + calculatedFuel + " but actual=" + actualFuel, transaction);
        }

        var context = SmcConfig.asContext(integratorFactory.createMockProcessor(transaction.getId()));
        //syntactical validation
        SmcContractTxProcessor processor = new SyntaxValidator(smartMethod.getMethodWithParams(), context);
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID");
            throw new AplUnacceptableTransactionValidationException("Syntax error: " + executionLog.toJsonString(), transaction);
        }
        log.debug("SMC: doStateIndependentValidation = VALID");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: call method. ");
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        Address address = new AplAddress(transaction.getRecipientId());
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        SmartContract smartContract = contractService.loadContract(
            address,
            transactionSender,
            transactionSender,
            new ContractFuel(address, attachment.getFuelLimit(), attachment.getFuelPrice())
        );
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        log.debug("Before processing: caller={} contract={} Fuel={}", smartContract.getCaller(), smartContract.getAddress(), smartContract.getFuel());
        var context = SmcConfig.asContext(integratorFactory.createProcessor(transaction, attachment,
            senderAccount, recipientAccount, getLedgerEvent())
        );

        executeContract(transaction, senderAccount, smartContract, new CallMethodTxProcessor(smartContract, smartMethod, context));

        //update contract and contract state
        contractService.updateContractState(smartContract);
        log.info("Called method {} on contract={}, txId={}, fuel={}, amountATM={}, tx.sender={}",
            smartMethod.getMethodWithParams(),
            smartContract.getAddress(), Long.toUnsignedString(transaction.getId()),
            smartContract.getFuel(), transaction.getAmountATM(), transactionSender);
        contractService.commit();
        log.trace("Changes were committed");
    }

    private Fee.FuelBasedFee getFuelBasedFee(Transaction transaction) {
        var fuelCalculator = getExecutionEnv().getPrice().forMethodCalling(BigInteger.valueOf(transaction.getAmountATM()));
        return new SmcFuelBasedFee(fuelCalculator);
    }
}