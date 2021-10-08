/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DGSRefundAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
class DGSRefundTransactionType extends DGSTransactionType {

    @Inject
    public DGSRefundTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_REFUND;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_REFUND;
    }

    @Override
    public String getName() {
        return "DigitalGoodsRefund";
    }

    @Override
    public DGSRefundAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DGSRefundAttachment(buffer);
    }

    @Override
    public DGSRefundAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DGSRefundAttachment(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        if (attachment.getRefundATM() < 0
            || attachment.getRefundATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
        }
        if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
            throw new AplException.NotValidException("Only text encrypted messages allowed");
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getRefundATM()) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -attachment.getRefundATM());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getRefundATM());
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        dgsService.refund(getLedgerEvent(), transaction.getId(), transaction.getSenderId(), attachment.getPurchaseId(), attachment.getRefundATM(), transaction.getEncryptedMessage());
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        DGSPurchase purchase = dgsService.getPurchase(attachment.getPurchaseId());
        if (purchase != null && (purchase.getBuyerId() != transaction.getRecipientId()
            || transaction.getSenderId() != purchase.getSellerId())) {
            throw new AplException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
        }
        if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundATM() != 0) {
            throw new AplException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
        }
        verifyAccountBalanceSufficiency(transaction, attachment.getRefundATM());
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DGSRefundAttachment attachment = (DGSRefundAttachment) transaction.getAttachment();
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DGS_REFUND, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
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
