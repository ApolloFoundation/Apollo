/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class AliasSellTransactionType extends MessagingTransactionType {
    private final AliasService aliasService;

    @Inject
    public AliasSellTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AliasService aliasService) {
        super(blockchainConfig, accountService);
        this.aliasService = aliasService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ALIAS_SELL;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ALIAS_SELL;
    }

    @Override
    public String getName() {
        return "AliasSell";
    }

    @Override
    public MessagingAliasSell parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAliasSell(buffer);
    }

    @Override
    public MessagingAliasSell parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAliasSell(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
        aliasService.sellAlias(transaction, attachment);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
        // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
        return isDuplicate(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Invalid sell alias transaction: " + transaction.getJSONObject());
        }
        final MessagingAliasSell attachment = (MessagingAliasSell) transaction.getAttachment();
        final String aliasName = attachment.getAliasName();
        if (aliasName == null || aliasName.length() == 0) {
            throw new AplException.NotValidException("Missing alias name");
        }
        long priceATM = attachment.getPriceATM();
        if (priceATM < 0 || priceATM > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid alias sell price: " + priceATM);
        }
        if (priceATM == 0) {
            if (GenesisImporter.CREATOR_ID == transaction.getRecipientId()) {
                throw new AplException.NotValidException("Transferring aliases to Genesis account not allowed");
            } else if (transaction.getRecipientId() == 0) {
                throw new AplException.NotValidException("Missing alias transfer recipient");
            }
        }
        final Alias alias = aliasService.getAliasByName(aliasName);
        if (alias == null) {
            throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
        } else if (alias.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Selling alias to Genesis not allowed");
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
