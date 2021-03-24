/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountProperty;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class AccountPropertyTransactionType extends MessagingTransactionType {
    private final AccountPropertyService accountPropertyService;

    @Inject
    public AccountPropertyTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountPropertyService accountPropertyService) {
        super(blockchainConfig, accountService);
        this.accountPropertyService = accountPropertyService;
    }

    private final Fee ACCOUNT_PROPERTY_FEE = new Fee.SizeBasedFee(getBlockchainConfig().getOneAPL(), getBlockchainConfig().getOneAPL(), 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
            return attachment.getValue().length();
        }
    };

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ACCOUNT_PROPERTY;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ACCOUNT_PROPERTY;
    }

    @Override
    public String getName() {
        return "AccountProperty";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return ACCOUNT_PROPERTY_FEE;
    }

    @Override
    public MessagingAccountProperty parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAccountProperty(buffer);
    }

    @Override
    public MessagingAccountProperty parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAccountProperty(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
        if (attachment.getProperty().length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH || attachment.getProperty().length() == 0 || attachment.getValue().length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
            throw new AplException.NotValidException("Invalid account property: " + attachment.getJSONObject());
        }
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Account property transaction cannot be used to send " + getBlockchainConfig().getCoinSymbol());
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Setting Genesis account properties not allowed");
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingAccountProperty attachment = (MessagingAccountProperty) transaction.getAttachment();
        accountPropertyService.setProperty(recipientAccount, transaction, senderAccount, attachment.getProperty(), attachment.getValue());
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
