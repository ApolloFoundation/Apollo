/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.CallMethodContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SandboxCallMethodValidationProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import com.apollocurrency.smc.data.type.Address;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcCallMethodTransactionType extends AbstractSmcTransactionType {

    protected final Fee CALL_CONTRACT_METHOD_FEE = new Fee.SizeBasedFee(
        Math.multiplyExact(10_000, getBlockchainConfig().getOneAPL()),
        Math.multiplyExact(1_000, getBlockchainConfig().getOneAPL()), MACHINE_WORD_SIZE) {
        public int getSize(Transaction transaction, Appendix appendage) {
            SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
            String smc = attachment.getMethodName() + " " + attachment.getMethodParams();
            //TODO ??? what about string compressing, something like: output = Compressor.deflate(input)
            return smc.length();
        }
    };

    @Inject
    public SmcCallMethodTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, ContractService contractService, SMCMachineFactory machineFactory) {
        super(blockchainConfig, accountService, contractService, machineFactory);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public @TransactionFee(FeeMarker.BASE_FEE) Fee getBaselineFee(Transaction transaction) {
        return CALL_CONTRACT_METHOD_FEE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SMC_CALL_METHOD;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateDependentValidation");
        Address address = new AplAddress(transaction.getRecipientId());
        if (!contractService.isContractExist(address)) {
            throw new AplException.NotCurrentlyValidException("Contract doesn't exist at address " + address);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateIndependentValidation");
        checkPrecondition(transaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        if (Strings.isNullOrEmpty(attachment.getMethodName())) {
            throw new AplException.NotCurrentlyValidException("Empty contract method name.");
        }
        //syntactical and semantic validation
        SmartContract smartContract = contractService.createNewContract(transaction);
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(transaction.getAmount())
            .build();
        SMCMachine smcMachine = machineFactory.createNewInstance();

        ContractTxProcessor processor = new SandboxCallMethodValidationProcessor(smcMachine, smartContract, smartMethod);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: call method.");
        checkPrecondition(transaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        Address address = new AplAddress(transaction.getRecipientId());
        SmartContract smartContract = contractService.loadContract(address);
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(transaction.getAmount())
            .build();
        SMCMachine smcMachine = machineFactory.createNewInstance();
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());
        ContractTxProcessor processor = new CallMethodContractTxProcessor(smcMachine, smartContract, smartMethod);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        //TODO refund remaining fuel
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        log.debug("After processing Address={} Fuel={}", smartContract.getAddress(), fuel);
        contractService.updateContractState(smartContract);
    }
}
