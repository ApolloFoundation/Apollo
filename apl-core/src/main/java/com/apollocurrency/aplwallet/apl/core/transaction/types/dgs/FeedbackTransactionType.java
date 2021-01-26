/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsFeedback;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class FeedbackTransactionType extends DigitalGoodsTransactionType {

    @Inject
    public FeedbackTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService, service);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_FEEDBACK;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_FEEDBACK;
    }

    @Override
    public String getName() {
        return "DigitalGoodsFeedback";
    }

    @Override
    public DigitalGoodsFeedback parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsFeedback(buffer);
    }

    @Override
    public DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DigitalGoodsFeedback(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
            throw new AplException.NotValidException("Missing feedback message");
        }
        if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
            throw new AplException.NotValidException("Only text encrypted messages allowed");
        }
        if (transaction.getMessage() != null && !transaction.getMessage().isText()) {
            throw new AplException.NotValidException("Only text public messages allowed");
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsFeedback attachment = (DigitalGoodsFeedback) transaction.getAttachment();
        dgsService.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsFeedback attachment = (DigitalGoodsFeedback) transaction.getAttachment();
        DGSPurchase purchase = dgsService.getPurchase(attachment.getPurchaseId());
        if (purchase != null && (purchase.getSellerId() != transaction.getRecipientId() || transaction.getSenderId() != purchase.getBuyerId())) {
            throw new AplException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
        }
        if (purchase == null || purchase.getEncryptedGoods() == null) {
            throw new AplException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
        }
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
