/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountInfo;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
public class AccountInfoTransactionType extends MessagingTransactionType {
    private final AccountInfoService accountInfoService;

    @Inject
    public AccountInfoTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountInfoService accountInfoService) {
        super(blockchainConfig, accountService);
        this.accountInfoService = accountInfoService;
    }

    private final Fee ACCOUNT_INFO_FEE = new Fee.SizeBasedFee(getBlockchainConfig().getOneAPL(), Math.multiplyExact(2, getBlockchainConfig().getOneAPL()), 32) {
        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
            return attachment.getName().length() + attachment.getDescription().length();
        }
    };

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ACCOUNT_INFO;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ACCOUNT_INFO;
    }

    @Override
    public String getName() {
        return "AccountInfo";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return ACCOUNT_INFO_FEE;
    }

    @Override
    public MessagingAccountInfo parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAccountInfo(buffer);
    }

    @Override
    public MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAccountInfo(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
        if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
            throw new AplException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingAccountInfo attachment = (MessagingAccountInfo) transaction.getAttachment();
        accountInfoService.updateAccountInfo(senderAccount, attachment.getName(), attachment.getDescription());
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return isDuplicate(getSpec(), getName(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
}
