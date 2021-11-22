/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.smc.contract.ContractSource;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.Languages;
import com.apollocurrency.smc.polyglot.language.SmartSource;
import com.apollocurrency.smc.polyglot.language.lib.Preprocessor;
import com.apollocurrency.smc.polyglot.language.validator.Matcher;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractToolServiceImpl implements ContractToolService {
    private final Blockchain blockchain;
    protected final SmcConfig smcConfig;
    private final Preprocessor preprocessor;
    private final Matcher codeMatcher;

    @Inject
    public SmcContractToolServiceImpl(Blockchain blockchain, SmcConfig smcConfig) {
        this.blockchain = blockchain;
        this.smcConfig = smcConfig;
        final LanguageContext languageContext = smcConfig.createLanguageContext();
        this.preprocessor = languageContext.getPreprocessor();
        this.codeMatcher = languageContext.getCodeMatcher();
    }

    @Override
    public boolean validateContractSource(String source) {
        //validate, match the source code with template by RegExp
        var rc = codeMatcher.match(source);
        log.debug("Validate smart contract source at height {}, rc={}, smc={}", blockchain.getHeight(), rc, source);
        return rc;
    }

    @Override
    public SmartSource createSmartSource(SmcPublishContractAttachment attachment) {
        return ContractSource.builder()
            .name(attachment.getContractName())
            .baseContract(attachment.getBaseContract())
            .sourceCode(attachment.getContractSource())
            .languageName(attachment.getLanguageName())
            .languageVersion(SimpleVersion.fromString(attachment.getLanguageVersion()))
            .build();
    }

    @Override
    public SmartSource createSmartSource(String name, String source, String languageName) {
        return ContractSource.builder()
            .name(name)
            .sourceCode(source)
            .languageName(languageName)
            .languageVersion(Languages.languageVersion(source))
            .baseContract(preprocessor.parseContractType(source).getType())
            .build();
    }

    @Override
    public SmartSource completeContractSource(SmartSource smartSource) {
        var processedSrc = preprocessor.process(smartSource);
        log.trace("Completed smartSource={}", processedSrc);
        return processedSrc;
    }

    @Override
    public SmartContract createNewContract(Transaction smcTransaction) {
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH) {
            throw new AplCoreContractViolationException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec()
                + ", expected " + TransactionTypes.TransactionTypeSpec.SMC_PUBLISH + ", transaction_id=" + smcTransaction.getStringId());
        }
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) smcTransaction.getAttachment();

        final Address contractAddress = new AplAddress(smcTransaction.getRecipientId());
        final Address transactionSender = new AplAddress(smcTransaction.getSenderId());
        final Address txId = new AplAddress(smcTransaction.getId());

        final SmartSource smartSource = createSmartSource(attachment);

        SmartContract contract = SmartContract.builder()
            .address(contractAddress)
            .owner(transactionSender)
            .originator(transactionSender)
            .caller(transactionSender)
            .txId(txId)
            .args(attachment.getConstructorParams())
            .code(smartSource)
            .status(ContractStatus.CREATED)
            .fuel(new ContractFuel(transactionSender, attachment.getFuelLimit(), attachment.getFuelPrice()))
            .build();

        log.debug("Created contract={}", contract);

        return contract;
    }

}
