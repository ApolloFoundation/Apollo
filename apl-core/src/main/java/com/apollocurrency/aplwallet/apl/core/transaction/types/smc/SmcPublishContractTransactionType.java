/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactoryCreator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcPostponedContractServiceImpl;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureVerifier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxValidator;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.PolyglotException;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Optional;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcPublishContractTransactionType extends AbstractSmcTransactionType {
    private final TxBContext txBContext;

    @Inject
    public SmcPublishContractTransactionType(BlockchainConfig blockchainConfig, Blockchain blockchain,
                                             AccountService accountService,
                                             SmcContractRepository contractRepository,
                                             ContractToolService contractToolService,
                                             SmcFuelValidator fuelValidator,
                                             SmcBlockchainIntegratorFactoryCreator integratorFactory,
                                             SmcConfig smcConfig) {
        super(blockchainConfig, blockchain, accountService, contractRepository, contractToolService, fuelValidator, integratorFactory, smcConfig);
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
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
    public AbstractAttachment parseAttachment(JSONObject attachmentData) {
        return new SmcPublishContractAttachment(attachmentData);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), TransactionTypes.TransactionTypeSpec.SMC_PUBLISH.name(), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) {
        var contractService = new SmcPostponedContractServiceImpl(contractRepository);
        log.debug("SMC: doStateDependentValidation = ...  txId={}", transaction.getStringId());
        Address address = new AplAddress(transaction.getId());
        if (contractService.isContractExist(address)) {
            log.debug("SMC: doStateDependentValidation = INVALID  txId={}", transaction.getStringId());
            throw new AplAcceptableTransactionValidationException("Contract already exists, address=" + address, transaction);
        }
        log.debug("SMC: doStateDependentValidation = VALID  txId={}", transaction.getStringId());
    }

    @Override
    public void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) {
        log.debug("SMC: doStateIndependentValidation = ... txId={}", transaction.getStringId());
        if (getBlockchainConfig().getCurrentConfig().getSmcMasterAccountPublicKey() == null) {
            throw new AplTransactionFeatureNotEnabledException("'Publish contract' transaction is disabled, cause masterAccountPublicKey is null", transaction);
        }
        //Verify multi-signature of the given transaction to avoid suspicious transactions
        if (!verifySignature(transaction)) {
            throw new AplUnacceptableTransactionValidationException("Multi-signature verification failed.", transaction);
        }

        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) abstractSmcAttachment;
        if (Strings.isNullOrEmpty(attachment.getContractName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract name.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getBaseContract())) {
            throw new AplUnacceptableTransactionValidationException("Empty base contract.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getContractSource())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract source.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getLanguageName())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract language name.", transaction);
        }
        if (Strings.isNullOrEmpty(attachment.getLanguageVersion())) {
            throw new AplUnacceptableTransactionValidationException("Empty contract language version.", transaction);
        }
        SmartContract smartContract = contractToolService.createNewContract(transaction);
        var context = smcConfig.asContext(blockchain.getHeight(),
            smartContract,
            integratorFactory.createMockProcessorFactory(transaction.getId())
        );
        var pcf = new SmcFuelBasedFee(context.getPrice().contractPublishing());
        BigInteger calculatedFuel = pcf.calcFuel(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            throw new AplUnacceptableTransactionValidationException("Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel(), transaction);
        }
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new PublishContractTxValidator(smartContract, context);
        try {
            processor.process();
        } catch (PolyglotException e) {
            log.debug("SMC: doStateIndependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplUnacceptableTransactionValidationException(e.getMessage(), transaction);
        }
        if (processor.getExecutionLog().hasError()) {
            log.debug("SMC: doStateIndependentValidation = INVALID txId={}", transaction.getStringId());
            throw new AplUnacceptableTransactionValidationException(processor.getExecutionLog().toJsonString(), transaction);
        }
        log.debug("SMC: doStateIndependentValidation = VALID txId={}", transaction.getStringId());
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.debug("SMC: applyAttachment: publish smart contract and call constructor, txId={}", transaction.getStringId());
        SmartContract smartContract = contractToolService.createNewContract(transaction);
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) transaction.getAttachment();
        var context = smcConfig.asContext(blockchain.getHeight(),
            smartContract,
            integratorFactory.createProcessorFactory(transaction, attachment, senderAccount, recipientAccount, getLedgerEvent())
        );
        log.debug("Before processing Address={} Fuel={}", smartContract.address(), smartContract.getFuel());
        var contractService = new SmcPostponedContractServiceImpl(contractRepository);

        executeContract(transaction, senderAccount, smartContract, new PublishContractTxProcessor(smartContract, context));

        //save contract and contract state
        contractService.saveContract(smartContract, transaction.getId(), transaction.getFullHash());
        log.info("Contract {} published init=[{}], txId={}, fuel={}, amountATM={}, owner={}",
            smartContract.address(), smartContract.getInitCode(), Long.toUnsignedString(transaction.getId()),
            smartContract.getFuel(), transaction.getAmountATM(), smartContract.getOwner());
        contractService.commitContractChanges(transaction);
        log.trace("Changes were committed, txId={}", transaction.getStringId());
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    /**
     * Returns the credential to verify the 'Publish smart-contract' transaction signature
     *
     * @param transaction given transaction
     * @return the credential to verify the given transaction signature
     */
    private MultiSigCredential getCredential(Transaction transaction) {
        var masterPK = getBlockchainConfig().getCurrentConfig().getSmcMasterAccountPublicKey();
        if (masterPK != null) {
            log.trace("Smc master credentials: publicKey={}", Convert.toHexString(masterPK));
            return new MultiSigCredential(2, masterPK, transaction.getSenderPublicKey());
        } else {
            throw new AplUnacceptableTransactionValidationException("Unknown master account public key.", transaction);
        }
    }

    public boolean verifySignature(Transaction transaction) {
        if (transaction.getSignature() == null) {
            return false;
        }
        Credential signatureCredential;
        Optional<SignatureVerifier> signatureVerifierOptional = SignatureToolFactory.selectValidator(transaction.getVersion());
        if (signatureVerifierOptional.isEmpty()) {
            log.error("Unsupported version: '{}' of the transaction: '{}'", transaction.getVersion(), transaction.getStringId());
            return false;
        }
        SignatureVerifier signatureVerifier = signatureVerifierOptional.get();
        signatureCredential = getCredential(transaction);
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), byteArrayTx);

        return signatureVerifier.verify(byteArrayTx.array(), transaction.getSignature(), signatureCredential);
    }
}
