/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.payment;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.ByteBuffer;
@Singleton
public class PrivatePaymentTransactionType extends PaymentTransactionType {
    @Inject
    public PrivatePaymentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT;
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
    protected void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
    }
}
