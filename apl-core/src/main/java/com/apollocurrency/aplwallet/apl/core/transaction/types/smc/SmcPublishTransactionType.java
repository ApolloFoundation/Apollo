/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcPublishTransactionType extends SmcTransactionType {
    private final AccountPublicKeyService accountPublicKeyService;
    private final Blockchain blockchain;

    @Inject
    public SmcPublishTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountPublicKeyService accountPublicKeyService, Blockchain blockchain) {
        super(blockchainConfig, accountService);
        this.accountPublicKeyService = accountPublicKeyService;
        this.blockchain = blockchain;
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
    public AbstractAttachment parseAttachment(RlpReader reader) throws AplException.NotValidException {
        return new SmcPublishContractAttachment(reader);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new SmcPublishContractAttachment(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.info("SMC: doStateDependentValidation");

    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        log.info("SMC: doStateIndependentValidation");

    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        log.info("SMC: applyAttachment");

    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
