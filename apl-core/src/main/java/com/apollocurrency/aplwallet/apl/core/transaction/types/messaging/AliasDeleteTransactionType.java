/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasDelete;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;


@Singleton
public class AliasDeleteTransactionType extends MessagingTransactionType {
    private final AliasService aliasService;

    @Inject
    public AliasDeleteTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AliasService aliasService) {
        super(blockchainConfig, accountService);
        this.aliasService = aliasService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ALIAS_DELETE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ALIAS_DELETE;
    }

    @Override
    public String getName() {
        return "AliasDelete";
    }

    @Override
    public MessagingAliasDelete parseAttachment(final ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAliasDelete(buffer);
    }

    @Override
    public MessagingAliasDelete parseAttachment(final JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAliasDelete(attachmentData);
    }

    @Override
    public void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
        final MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
        aliasService.deleteAlias(attachment.getAliasName());
    }

    @Override
    public boolean isDuplicate(final Transaction transaction, final Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
        // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
        return isDuplicate(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(final Transaction transaction) throws AplException.ValidationException {
        final MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
        final String aliasName = attachment.getAliasName();
        final Alias alias = aliasService.getAliasByName(aliasName);
        if (alias == null) {
            throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
        } else if (alias.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        final MessagingAliasDelete attachment = (MessagingAliasDelete) transaction.getAttachment();
        final String aliasName = attachment.getAliasName();
        if (aliasName == null || aliasName.length() == 0) {
            throw new AplException.NotValidException("Missing alias name");
        }
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
