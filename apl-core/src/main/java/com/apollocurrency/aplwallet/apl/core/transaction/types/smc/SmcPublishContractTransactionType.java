/*
 * Copyright (c) 2018-2020. Apollo Foundation.
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
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxValidator;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.contract.SmartContract;
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
public class SmcPublishContractTransactionType extends AbstractSmcTransactionType {
    protected final SmcFuelBasedFee publishContractFee = new SmcFuelBasedFee(getExecutionEnv().getPrice().forContractPublishing());

    @Inject
    public SmcPublishContractTransactionType(BlockchainConfig blockchainConfig, AccountService accountService,
                                             SmcContractService contractService,
                                             FuelValidator fuelValidator,
                                             SmcBlockchainIntegratorFactory integratorFactory,
                                             SmcConfig smcConfig) {
        super(blockchainConfig, accountService, contractService, fuelValidator, integratorFactory, smcConfig);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SMC_PUBLISH;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new SmcPublishContractAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(RlpReader reader) {
        return new SmcPublishContractAttachment(reader);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) {
        return new SmcPublishContractAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), Long.toUnsignedString(transaction.getId()), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) {
        log.debug("SMC: doStateDependentValidation = ...");
        Address address = new AplAddress(transaction.getRecipientId());
        if (contractService.isContractExist(address)) {
            log.debug("SMC: doStateDependentValidation = INVALID");
            throw new AplAcceptableTransactionValidationException("Contract already exists, address=" + address, transaction);
        }
        log.debug("SMC: doStateDependentValidation = VALID");
    }

    @Override
    public void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) {
        log.debug("SMC: doStateIndependentValidation = ...");
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) abstractSmcAttachment;
        if (Strings.isNullOrEmpty(attachment.getContractName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract name.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getContractSource())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract source.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getLanguageName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract language name.", transaction);
        }
        SmartContract smartContract = contractService.createNewContract(transaction);
        if (!contractService.validateContractSource(smartContract.getCode())) {
            throw new AplUnacceptableTransactionValidationException("The contract source code doesn't match the contract template code.", transaction);
        }
        BigInteger calculatedFuel = publishContractFee.calcFuel(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            throw new AplUnacceptableTransactionValidationException("Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel(), transaction);
        }
        var context = SmcConfig.asContext(integratorFactory.createMockProcessor(transaction.getId()));
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new PublishContractTxValidator(smartContract, context);
        var executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID");
            throw new AplUnacceptableTransactionValidationException(executionLog.toJsonString(), transaction);
        }
        log.debug("SMC: doStateIndependentValidation = VALID");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: publish smart contract and call constructor.");
        SmartContract smartContract = contractService.createNewContract(transaction);
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) transaction.getAttachment();
        var context = SmcConfig.asContext(integratorFactory.createProcessor(transaction, attachment,
            senderAccount, recipientAccount, getLedgerEvent()));
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());

        executeContract(transaction, senderAccount, smartContract, new PublishContractTxProcessor(smartContract, context));

        //save contract and contract state
        contractService.saveContract(smartContract);
        log.info("Contract {} published init=[{}], txId={}, fuel={}, amountATM={}, owner={}",
            smartContract.getAddress(), smartContract.getInitCode(), Long.toUnsignedString(transaction.getId()),
            smartContract.getFuel(), transaction.getAmountATM(), smartContract.getOwner());
    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }
}
