/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.payment;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
@Singleton
public class PrivatePaymentTransactionType extends PaymentTransactionType {
    @Inject
    public PrivatePaymentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public final byte getSubtype() {
        return TransactionTypes.SUBTYPE_PAYMENT_PRIVATE_PAYMENT;
    }

    @Override
    public final LedgerEvent getLedgerEvent() {
        return LedgerEvent.PRIVATE_PAYMENT;
    }

    @Override
    public String getName() {
        return "PrivatePayment";
    }

    @Override
    public EmptyAttachment parseAttachment(ByteBuffer buffer) {
        return Attachment.PRIVATE_PAYMENT;
    }

    @Override
    public EmptyAttachment parseAttachment(JSONObject attachmentData) {
        return Attachment.PRIVATE_PAYMENT;
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid private payment");
        }
    }
}
