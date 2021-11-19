/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class ArbitraryMessageTransactionType extends MessagingTransactionType {

    @Inject
    public ArbitraryMessageTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ARBITRARY_MESSAGE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ARBITRARY_MESSAGE;
    }

    @Override
    public String getName() {
        return "ArbitraryMessage";
    }

    @Override
    public EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return Attachment.ARBITRARY_MESSAGE;
    }

    @Override
    public EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return Attachment.ARBITRARY_MESSAGE;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {

    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        Attachment attachment = transaction.getAttachment();
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Sending messages to Genesis not allowed.");
        }
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
