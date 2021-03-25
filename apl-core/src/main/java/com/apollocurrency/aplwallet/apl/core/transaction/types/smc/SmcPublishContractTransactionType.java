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
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.ContractTxProcessorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.data.type.Address;
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
        Math.multiplyExact(150_000, getBlockchainConfig().getOneAPL()),
        Math.multiplyExact(1_000, getBlockchainConfig().getOneAPL()), MACHINE_WORD_SIZE) {
        public int getSize(Transaction transaction, Appendix appendage) {
            SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) transaction.getAttachment();
            String smc = attachment.getContractSource() + String.join(",", attachment.getConstructorParams());
            //TODO ??? what about string compressing, something like: output = Compressor.deflate(input)
            return smc.length();
        }
    };

    @Inject
    public SmcPublishContractTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, ContractService contractService, ContractTxProcessorFactory processorFactory) {
        super(blockchainConfig, accountService, contractService, processorFactory);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public @TransactionFee(FeeMarker.BASE_FEE) Fee getBaselineFee(Transaction transaction) {
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
        ContractTxProcessor processor = processorFactory.createContractValidationProcessor(transaction);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.NotCurrentlyValidException(executionLog.toJsonString());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment");
        ContractTxProcessor processor = processorFactory.createPublishContractProcessor(transaction);
        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            throw new AplException.SMCProcessingException(executionLog.toJsonString());
        }
        SmartContract smartContract = processor.smartContract();

    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }
}
