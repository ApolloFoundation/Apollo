/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasBuy;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class AliasBuyTransactionType extends MessagingTransactionType {
    private final AliasService aliasService;

    @Inject
    public AliasBuyTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AliasService aliasService) {
        super(blockchainConfig, accountService);
        this.aliasService = aliasService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ALIAS_BUY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ALIAS_BUY;
    }

    @Override
    public String getName() {
        return "AliasBuy";
    }

    @Override
    public MessagingAliasBuy parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAliasBuy(buffer);
    }

    @Override
    public MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAliasBuy(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        final MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
        final String aliasName = attachment.getAliasName();
        aliasService.changeOwner(transaction.getSenderId(), aliasName);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
        // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
        return isDuplicate(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        final MessagingAliasBuy attachment = (MessagingAliasBuy) transaction.getAttachment();
        final String aliasName = attachment.getAliasName();
        final Alias alias = aliasService.getAliasByName(aliasName);
        if (alias == null) {
            throw new AplException.NotCurrentlyValidException("No such alias: " + aliasName);
        } else if (alias.getAccountId() != transaction.getRecipientId()) {
            throw new AplException.NotCurrentlyValidException("Alias is owned by account other than recipient: " + Long.toUnsignedString(alias.getAccountId()));
        }
        AliasOffer offer = aliasService.getOffer(alias);
        if (offer == null) {
            throw new AplException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
        }
        if (transaction.getAmountATM() < offer.getPriceATM()) {
            String msg = "Price is too low for: " + aliasName + " (" + transaction.getAmountATM() + " < " + offer.getPriceATM() + ")";
            throw new AplException.NotCurrentlyValidException(msg);
        }
        if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": " + Long.toUnsignedString(transaction.getSenderId()) + " expected: " + Long.toUnsignedString(offer.getBuyerId()));
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {

    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
