/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.AplBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.AplMachine;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.PublishContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SandboxContractValidationProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
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
public class SmcPublishContractTransactionType extends AbstractSmcTransactionType {
    protected final Fee PUBLISH_CONTRACT_FEE = new Fee.SizeBasedFee(
        Math.multiplyExact(1_500, getBlockchainConfig().getOneAPL()),//APL
        10,//ATM
        1)//BYTE
    {
        public int getSize(Transaction transaction, Appendix appendage) {
            SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) transaction.getAttachment();
            int size = attachment.getContractSource().length();
            if (attachment.getConstructorParams() != null) {
                size += attachment.getConstructorParams().length();
            }
            //TODO ??? what about string compressing, something like: output = Compressor.deflate(input)
            return size;
        }
    };

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
        return PUBLISH_CONTRACT_FEE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.SMC_PUBLISH;
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
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateDependentValidation");
        Address address = new AplAddress(transaction.getRecipientId());
        if (contractService.isContractExist(address)) {
            throw new AplException.NotCurrentlyValidException("Contract already exists at address " + address);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.debug("SMC: doStateIndependentValidation");
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
        //syntactical and semantic validation
        SmartContract smartContract = contractService.createNewContract(transaction);
        BlockchainIntegrator integrator = integratorFactory.createMockInstance(transaction.getId());
        SMCMachine smcMachine = new AplMachine(SmcConfig.createLanguageContext(), integrator);

        ContractTxProcessor processor = new SandboxContractValidationProcessor(smcMachine, smartContract);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: publish smart contract and call constructor.");
        checkPrecondition(transaction);
        SmartContract smartContract = contractService.createNewContract(transaction);
        BlockchainIntegrator integrator = integratorFactory.createInstance(transaction.getId(), senderAccount, recipientAccount, getLedgerEvent());
        SMCMachine smcMachine = new AplMachine(SmcConfig.createLanguageContext(), integrator);
        log.debug("Before processing Address={} Fuel={}", smartContract.getAddress(), smartContract.getFuel());
        ContractTxProcessor processor = new PublishContractTxProcessor(smcMachine, smartContract);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        log.debug("After processing Address={} Fuel={}", smartContract.getAddress(), fuel);
        if (fuel.refundedFee().signum() > 0) {
            //refund remaining fuel
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), 0, fuel.refundedFee().longValueExact());
        }
        //save contract and contract state
        contractService.saveContract(smartContract);
    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }
}
