/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.CallMethodTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.CallMethodTxValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SyntaxValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.ContractNotFoundException;
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
                                        SmcContractService contractService,
                                        FuelValidator fuelValidator,
                                        SmcBlockchainIntegratorFactory integratorFactory,
                                        SmcConfig smcConfig) {
        super(blockchainConfig, accountService, contractService, fuelValidator, integratorFactory, smcConfig);
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
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new SmcCallMethodAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(RlpReader reader) throws AplException.NotValidException {
        return new SmcCallMethodAttachment(reader);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new SmcCallMethodAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        //there is only one contract execution in the same block, contract address == transaction.getRecipientId()
        return isDuplicate(getSpec(), Long.toUnsignedString(transaction.getRecipientId()), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateDependentValidation = ...");
        Address address = new AplAddress(transaction.getRecipientId());
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        SmartContract smartContract;
        try {
            smartContract = contractService.loadContract(
                new AplAddress(transaction.getRecipientId()),
                new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
            );
            smartContract.setSender(new AplAddress(transaction.getSenderId()));
        } catch (ContractNotFoundException e) {
            throw new AplException.NotCurrentlyValidException("Contract doesn't exist at address " + address);
        }
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new CallMethodTxValidator(
            smartContract,
            smartMethod,
            integratorFactory.createMockProcessor(transaction.getId()),
            smcConfig
        );
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("SMC: doStateDependentValidation = INVALID");
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
        log.debug("SMC: doStateDependentValidation = VALID");
    }

    @Override
    public void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) throws AplException.ValidationException {
        log.debug("SMC: doStateIndependentValidation = ...");
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) abstractSmcAttachment;
        if (Strings.isNullOrEmpty(attachment.getMethodName())) {
            throw new AplException.NotCurrentlyValidException("Empty contract method name.");
        }
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        BigInteger calculatedFuel = getFuelBasedFee(transaction).calcFuel(smartMethod);
        Fuel actualFuel = new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice());
        if (!actualFuel.tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, actualFuel);
            throw new AplException.NotCurrentlyValidException("Not enough fuel to execute this transaction, expected="
                + calculatedFuel + " but actual=" + actualFuel);
        }
        //syntactical validation
        SmcContractTxProcessor processor = new SyntaxValidator(
            smartMethod.getMethodWithParams(),
            integratorFactory.createMockProcessor(transaction.getId()),
            smcConfig
        );
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID");
            throw new AplException.NotCurrentlyValidException("Syntax error: " + executionLog.toJsonString());
        }
        log.debug("SMC: doStateIndependentValidation = VALID");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: call method. ");
        checkPrecondition(transaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        Address address = new AplAddress(transaction.getRecipientId());

        SmartContract smartContract = contractService.loadContract(
            address,
            new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
        );
        smartContract.setSender(new AplAddress(transaction.getSenderId()));
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();

        BlockchainIntegrator integrator = integratorFactory.createProcessor(transaction, smartContract.getAddress(), attachment, senderAccount, recipientAccount, getLedgerEvent());
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());
        SmcContractTxProcessor processor = new CallMethodTxProcessor(smartContract, smartMethod, integrator, smcConfig);
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.error(executionLog.toJsonString());
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        processor.commit();
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        log.debug("After processing Address={} Fuel={}", smartContract.getAddress(), fuel);
        refundRemaining(transaction, senderAccount, fuel);
        contractService.updateContractState(smartContract);
    }

    private Fee.FuelBasedFee getFuelBasedFee(Transaction transaction) {
        var fuelCalculator = getExecutionEnv().getPrice().forMethodCalling(BigInteger.valueOf(transaction.getAmountATM()));
        return new SmcFuelBasedFee(fuelCalculator);
    }
}
