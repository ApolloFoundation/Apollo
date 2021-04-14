/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.AplBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.PublishContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SandboxContractValidationProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.FuelCost;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.operation.OperationProcessor;
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
public class SmcPublishContractTransactionType extends AbstractSmcTransactionType {
    protected static final FuelBasedFee PUBLISH_CONTRACT_FEE = new FuelBasedFee(FuelCost.F_PUBLISH);

    @Inject
    public SmcPublishContractTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, ContractService contractService, AplBlockchainIntegratorFactory integratorFactory) {
        super(blockchainConfig, accountService, contractService, integratorFactory);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        //TODO calculate the required fuel value by executing the contract
        return PUBLISH_CONTRACT_FEE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SMC_PUBLISH;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new SmcPublishContractAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(RlpReader reader) throws AplException.NotValidException {
        return new SmcPublishContractAttachment(reader);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new SmcPublishContractAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), Long.toUnsignedString(transaction.getId()), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateDependentValidation = ...");
        Address address = new AplAddress(transaction.getRecipientId());
        if (contractService.isContractExist(address)) {
            log.debug("SMC: doStateDependentValidation = INVALID");
            throw new AplException.NotCurrentlyValidException("Contract already exists at address " + address);
        }
        log.debug("SMC: doStateDependentValidation = VALID");
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateIndependentValidation = ...");
        checkPrecondition(transaction);
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) transaction.getAttachment();
        if (Strings.isNullOrEmpty(attachment.getContractName())) {
            throw new AplException.NotCurrentlyValidException("Empty contract name.");
        }
        if (Strings.isNullOrEmpty(attachment.getContractSource())) {
            throw new AplException.NotCurrentlyValidException("Empty contract source.");
        }
        if (Strings.isNullOrEmpty(attachment.getLanguageName())) {
            throw new AplException.NotCurrentlyValidException("Empty contract language name.");
        }
        SmartContract smartContract = contractService.createNewContract(transaction);
        BigInteger calculatedFuel = PUBLISH_CONTRACT_FEE.calcFuel(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            throw new AplException.NotCurrentlyValidException("Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel());
        }
        //syntactical and semantic validation
        OperationProcessor integrator = integratorFactory.createMockProcessor(transaction.getId());
        ContractTxProcessor processor = new SandboxContractValidationProcessor(smartContract, integrator);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID");
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
        log.debug("SMC: doStateIndependentValidation = VALID");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: publish smart contract and call constructor.");
        checkPrecondition(transaction);
        SmartContract smartContract = contractService.createNewContract(transaction);
        OperationProcessor integrator = integratorFactory.createProcessor(transaction.getId(), senderAccount, recipientAccount, getLedgerEvent());
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());
        ContractTxProcessor processor = new PublishContractTxProcessor(smartContract, integrator);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        log.debug("After processing Address={} Fuel={}", smartContract.getAddress(), fuel);
        refundRemaining(transaction, senderAccount, fuel);
        //save contract and contract state
        contractService.saveContract(smartContract);
    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }
}
