/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.control;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AccountControlEffectiveBalanceLeasing;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
@Singleton
public class EffectiveBalanceLeasingTransactionType extends AccountControlTransactionType {
    private final AccountLeaseService accountLeaseService;

    @Inject
    public EffectiveBalanceLeasingTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountLeaseService accountLeaseService) {
        super(blockchainConfig, accountService);
        this.accountLeaseService = accountLeaseService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.EFFECTIVE_BALANCE_LEASING;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
    }

    @Override
    public String getName() {
        return "EffectiveBalanceLeasing";
    }

    @Override
    public AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new AccountControlEffectiveBalanceLeasing(buffer);
    }

    @Override
    public AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new AccountControlEffectiveBalanceLeasing(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        AccountControlEffectiveBalanceLeasing attachment = (AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
        Account sender = getAccountService().getAccount(transaction.getSenderId());
        accountLeaseService.leaseEffectiveBalance(sender, transaction.getRecipientId(), attachment.getPeriod());
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        AccountControlEffectiveBalanceLeasing attachment = (AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
        if (transaction.getSenderId() == transaction.getRecipientId()) {
            throw new AplException.NotValidException("Account cannot lease balance to itself");
        }
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Transaction amount must be 0 for effective balance leasing");
        }
        if (attachment.getPeriod() < getBlockchainConfig().getLeasingDelay() || attachment.getPeriod() > 65535) {
            throw new AplException.NotValidException("Invalid effective balance leasing period: " + attachment.getPeriod());
        }
        byte[] recipientPublicKey = getAccountService().getPublicKeyByteArray(transaction.getRecipientId());
        if (recipientPublicKey == null) {
            throw new AplException.NotCurrentlyValidException("Invalid effective balance leasing: " + " recipient account " + Long.toUnsignedString(transaction.getRecipientId()) + " not found or no public key published");
        }
        if (transaction.getRecipientId() == GenesisImporter.CREATOR_ID) {
            throw new AplException.NotValidException("Leasing to Genesis account not allowed");
        }
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
