/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.AplBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.AplMachine;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.CallMethodContractTxProcessor;
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
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.data.type.Address;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcCallMethodTransactionType extends AbstractSmcTransactionType {

    protected final Fee CALL_CONTRACT_METHOD_FEE = new Fee.SizeBasedFee(
        Math.multiplyExact(100, getBlockchainConfig().getOneAPL()),
        Math.multiplyExact(1_000, getBlockchainConfig().getOneAPL()), MACHINE_WORD_SIZE) {
        public int getSize(Transaction transaction, Appendix appendage) {
            SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
            String smc = attachment.getMethodName() + " " + attachment.getMethodParams();
            //TODO ??? what about string compressing, something like: output = Compressor.deflate(input)
            return smc.length();
        }
    };

    @Inject
    public SmcCallMethodTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, ContractService contractService, AplBlockchainIntegratorFactory integratorFactory) {
        super(blockchainConfig, accountService, contractService, integratorFactory);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return CALL_CONTRACT_METHOD_FEE;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateDependentValidation = ...");
        Address address = new AplAddress(transaction.getRecipientId());
        if (!contractService.isContractExist(address)) {
            throw new AplException.NotCurrentlyValidException("Contract doesn't exist at address " + address);
        }
        log.debug("SMC: doStateDependentValidation = VALID");
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateIndependentValidation = ...");
        checkPrecondition(transaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        if (Strings.isNullOrEmpty(attachment.getMethodName())) {
            throw new AplException.NotCurrentlyValidException("Empty contract method name.");
        }
        SmartContract smartContract = contractService.loadContract(new AplAddress(transaction.getRecipientId()));
        //syntactical and semantic validation
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        BlockchainIntegrator integrator = integratorFactory.createMockInstance(transaction.getId());
        SMCMachine smcMachine = new AplMachine(SmcConfig.createLanguageContext(), integrator);
        log.debug("Created virtual machine for the contract validation, smcMachine={}", smcMachine);

        ContractTxProcessor processor = new SandboxCallMethodValidationProcessor(smcMachine, smartContract, smartMethod);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
        log.debug("SMC: doStateIndependentValidation = VALID");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: call method.");
        checkPrecondition(transaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        Address address = new AplAddress(transaction.getRecipientId());
        SmartContract smartContract = contractService.loadContract(address);
        smartContract.setFuel(new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice()));
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        BlockchainIntegrator integrator = integratorFactory.createInstance(transaction.getId(), senderAccount, recipientAccount, getLedgerEvent());
        SMCMachine smcMachine = new AplMachine(SmcConfig.createLanguageContext(), integrator);
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());
        ContractTxProcessor processor = new CallMethodContractTxProcessor(smcMachine, smartContract, smartMethod);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            log.error(executionLog.toJsonString());
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        log.debug("After processing Address={} Fuel={}", smartContract.getAddress(), fuel);
        if (fuel.refundedFee().signum() > 0) {
            //refund remaining fuel
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), 0, fuel.refundedFee().longValueExact());
        }
        contractService.updateContractState(smartContract);
    }
}
