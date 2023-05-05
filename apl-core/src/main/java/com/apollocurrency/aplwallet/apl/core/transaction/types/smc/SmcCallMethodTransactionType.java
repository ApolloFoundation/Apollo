/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcPostponedContractServiceImpl;
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
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.OperationPrice;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.PolyglotException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
    public SmcCallMethodTransactionType(BlockchainConfig blockchainConfig, Blockchain blockchain,
                                        AccountService accountService,
                                        SmcContractRepository contractService,
                                        ContractToolService contractToolService,
                                        SmcFuelValidator fuelValidator,
                                        SmcBlockchainIntegratorFactory integratorFactory,
                                        SmcConfig smcConfig) {
        super(blockchainConfig, blockchain, accountService, contractService, contractToolService, fuelValidator, integratorFactory, smcConfig);
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
        log.debug("SMC: doStateDependentValidation = ... txId={}", transaction.getStringId());
        Address address = new AplAddress(transaction.getRecipientId());
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        var contractService = new SmcPostponedContractServiceImpl(contractRepository);
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
        var context = smcConfig.asContext(blockchain.getHeight(),
            smartContract,
            integratorFactory.createMockProcessor(transaction.getId())
        );
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new CallMethodTxValidator(smartContract, smartMethod, context);
        try {
            processor.process();
        } catch (PolyglotException e) {
            log.debug("SMC: doStateDependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplAcceptableTransactionValidationException(e.getMessage(), transaction);
        }
        if (processor.getExecutionLog().hasError()) {
            log.debug("SMC: doStateDependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplAcceptableTransactionValidationException(processor.getExecutionLog().toJsonString(), transaction);
        }
        log.debug("SMC: doStateDependentValidation = VALID txId={}", transaction.getStringId());
    }

    @Override
    public void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) {
        log.debug("SMC: doStateIndependentValidation = ... txId={}", transaction.getStringId());
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) abstractSmcAttachment;
        if (StringUtils.isBlank(attachment.getMethodName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract method name.", transaction);
        }
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();

        Fuel actualFuel = new ContractFuel(new AplAddress(transaction.getRecipientId()), attachment.getFuelLimit(), attachment.getFuelPrice());
        var context = smcConfig.asContext(blockchain.getHeight(),
            actualFuel,
            integratorFactory.createMockProcessor(transaction.getId())
        );

        BigInteger calculatedFuel = getFuelBasedFee(context.getPrice(), transaction.getAmountATM()).calcFuel(smartMethod);

        if (!actualFuel.tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, actualFuel);
            throw new AplUnacceptableTransactionValidationException("Not enough fuel to execute this transaction, expected="
                + calculatedFuel + " but actual=" + actualFuel, transaction);
        }

        //syntactical validation
        SmcContractTxProcessor processor = new SyntaxValidator(smartMethod.getMethodWithParams(), context);
        try {
            processor.process();
        } catch (PolyglotException e) {
            log.debug("SMC: doStateIndependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplUnacceptableTransactionValidationException(e.getMessage(), transaction);
        }
        if (processor.getExecutionLog().hasError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplUnacceptableTransactionValidationException("Syntax error: " + processor.getExecutionLog().toJsonString(), transaction);
        }
        log.debug("SMC: doStateIndependentValidation = VALID txId={}", transaction.getStringId());
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: call method txId={}", transaction.getStringId());
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        Address address = new AplAddress(transaction.getRecipientId());
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        var contractService = new SmcPostponedContractServiceImpl(contractRepository);
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
        var context = smcConfig.asContext(blockchain.getHeight(),
            smartContract,
            integratorFactory.createProcessor(transaction, attachment, senderAccount, recipientAccount, getLedgerEvent())
        );

        executeContract(transaction, senderAccount, smartContract, new CallMethodTxProcessor(smartContract, smartMethod, context));

        //update contract and contract state
        contractService.updateContractState(smartContract);
        log.info("Called method {} on contract={}, txId={}, fuel={}, amountATM={}, tx.sender={}",
            smartMethod.getMethodWithParams(),
            smartContract.getAddress(), transaction.getStringId(),
            smartContract.getFuel(), transaction.getAmountATM(), transactionSender);
        contractService.commitContractChanges(transaction);
        log.trace("Changes were committed, txId={}", transaction.getStringId());
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    private Fee.FuelBasedFee getFuelBasedFee(OperationPrice price, long amount) {
        var fuelCalculator = price.methodCalling(BigInteger.valueOf(amount));
        return new SmcFuelBasedFee(fuelCalculator);
    }
}
