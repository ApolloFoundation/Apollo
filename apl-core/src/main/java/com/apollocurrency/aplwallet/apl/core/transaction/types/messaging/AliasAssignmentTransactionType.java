/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.messaging;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
public class AliasAssignmentTransactionType extends MessagingTransactionType {

    private final AliasService aliasService;

    @Inject
    public AliasAssignmentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AliasService aliasService) {
        super(blockchainConfig, accountService);
        this.aliasService = aliasService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ALIAS_ASSIGNMENT;
    }

    @Override
    public String getName() {
        return "AliasAssignment";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return getFeeFactory().createSizeBased(BigDecimal.valueOf(2), BigDecimal.valueOf(2), (tx, appendage) -> {
            MessagingAliasAssignment attachment = (MessagingAliasAssignment) tx.getAttachment();
            return attachment.getAliasName().length() + attachment.getAliasURI().length();
        });
    }

    @Override
    public MessagingAliasAssignment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MessagingAliasAssignment(buffer);
    }

    @Override
    public MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MessagingAliasAssignment(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
        aliasService.addOrUpdateAlias(transaction, attachment);
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
        return isDuplicate(getSpec(), attachment.getAliasName().toLowerCase(), duplicates, true);
    }

    @Override
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return aliasService.getAliasByName(((MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null && isDuplicate(getSpec(), "", duplicates, true);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
        String normalizedAlias = attachment.getAliasName().toLowerCase();
        Alias alias = aliasService.getAliasByName(normalizedAlias);
        if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        MessagingAliasAssignment attachment = (MessagingAliasAssignment) transaction.getAttachment();
        if (attachment.getAliasName().length() == 0 || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
            throw new AplException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
        }
        String normalizedAlias = attachment.getAliasName().toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                throw new AplException.NotValidException("Invalid alias name: " + normalizedAlias);
            }
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
