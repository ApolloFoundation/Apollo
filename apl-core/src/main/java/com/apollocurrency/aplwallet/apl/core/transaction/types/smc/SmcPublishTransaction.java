/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SmcPublishTransaction extends SmcTransaction {

    public SmcPublishTransaction(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
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
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return Attachment.SMC_PUBLISH;
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return Attachment.SMC_PUBLISH;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {

    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {

    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

}
