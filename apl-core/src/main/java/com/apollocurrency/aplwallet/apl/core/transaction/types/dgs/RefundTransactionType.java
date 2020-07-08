/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsRefund;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;
@Singleton
class RefundTransactionType extends DigitalGoodsTransactionType {

    @Inject
    public RefundTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
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
    public DigitalGoodsRefund parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsRefund(buffer);
    }

    @Override
    public DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DigitalGoodsRefund(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= attachment.getRefundATM()) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -attachment.getRefundATM());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getRefundATM());
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
        dgsService.refund(getLedgerEvent(), transaction.getId(), transaction.getSenderId(), attachment.getPurchaseId(), attachment.getRefundATM(), transaction.getEncryptedMessage());
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
        DGSPurchase purchase = dgsService.getPurchase(attachment.getPurchaseId());
        if (attachment.getRefundATM() < 0
            || attachment.getRefundATM() > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()
            || (purchase != null && (purchase.getBuyerId() != transaction.getRecipientId()
            || transaction.getSenderId() != purchase.getSellerId()))) {
            throw new AplException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
        }
        if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
            throw new AplException.NotValidException("Only text encrypted messages allowed");
        }
        if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundATM() != 0) {
            throw new AplException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DigitalGoodsRefund attachment = (DigitalGoodsRefund) transaction.getAttachment();
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
