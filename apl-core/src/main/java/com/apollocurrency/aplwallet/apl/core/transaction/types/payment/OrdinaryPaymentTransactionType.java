/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.payment;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class OrdinaryPaymentTransactionType extends PaymentTransactionType {
    @Inject
    public OrdinaryPaymentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT;
    }


    @Override
    public final LedgerEvent getLedgerEvent() {
        return LedgerEvent.ORDINARY_PAYMENT;
    }

    @Override
    public String getName() {
        return "OrdinaryPayment";
    }

    @Override
    public EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return Attachment.ORDINARY_PAYMENT;
    }

    @Override
    public EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return Attachment.ORDINARY_PAYMENT;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid ordinary payment");
        }
    }
}
