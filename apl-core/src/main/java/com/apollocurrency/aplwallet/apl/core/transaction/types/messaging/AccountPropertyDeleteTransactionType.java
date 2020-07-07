/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountPropertyDelete;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
@Singleton
public class AccountPropertyDeleteTransactionType extends MessagingTransactionType {
    private final AccountPropertyService propertyService;

    @Inject
    public AccountPropertyDeleteTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountPropertyService propertyService) {
        super(blockchainConfig, accountService);
        this.propertyService = propertyService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ACCOUNT_PROPERTY_DELETE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ACCOUNT_PROPERTY_DELETE;
    }

    @Override
    public String getName() {
        return "AccountPropertyDelete";
    }

    @Override
    public MessagingAccountPropertyDelete parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAccountPropertyDelete(buffer);
    }

    @Override
    public MessagingAccountPropertyDelete parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAccountPropertyDelete(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        MessagingAccountPropertyDelete attachment = (MessagingAccountPropertyDelete) transaction.getAttachment();
        AccountProperty accountProperty = propertyService.getProperty(attachment.getPropertyId());
        if (accountProperty == null) {
            throw new AplException.NotCurrentlyValidException("No such property " + Long.toUnsignedString(attachment.getPropertyId()));
        }
        if (accountProperty.getRecipientId() != transaction.getSenderId() && accountProperty.getSetterId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " cannot delete property " + Long.toUnsignedString(attachment.getPropertyId()));
        }
        if (accountProperty.getRecipientId() != transaction.getRecipientId()) {
            throw new AplException.NotValidException("Account property " + Long.toUnsignedString(attachment.getPropertyId()) + " does not belong to " + Long.toUnsignedString(transaction.getRecipientId()));
        }
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Account property transaction cannot be used to send " + getBlockchainConfig().getCoinSymbol());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Deleting Genesis account properties not allowed");
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingAccountPropertyDelete attachment = (MessagingAccountPropertyDelete) transaction.getAttachment();
        propertyService.deleteProperty(senderAccount, attachment.getPropertyId());
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
}
