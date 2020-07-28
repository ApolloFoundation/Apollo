/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.dex;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderCancelAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.DEX_CANCEL_ORDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.DEX_CONTRACT;

@Singleton
public class DexCancelOrderTransaction extends DexTransactionType {


    @Inject
    public DexCancelOrderTransaction(BlockchainConfig blockchainConfig, AccountService accountService, DexService dexService) {
        super(blockchainConfig, accountService, dexService);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return DEX_CANCEL_ORDER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexOrderCancelAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexOrderCancelAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        long orderTransactionId = attachment.getOrderId();

        DexOrder order = dexService.getOrder(orderTransactionId);
        if (order == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + orderTransactionId);
        }

        if (!order.getAccountId().equals(transaction.getSenderId())) {
            throw new AplException.NotCurrentlyValidException("Can cancel only your orders.");
        }

        if (!OrderStatus.OPEN.equals(order.getStatus())) {
            throw new AplException.NotCurrentlyValidException("Can cancel only Open orders. Order Id/Tx: " + order.getId() + "/" + Long.toUnsignedString(order.getId())
                + ", order status: " + order.getStatus() + " , Cancel Tx dbId:" + Long.toUnsignedString(transaction.getId()) + ", BlockId: " + Long.toUnsignedString(transaction.getECBlockId()));
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        DexOrder order = dexService.getOrder(attachment.getOrderId());

        try {
            dexService.tryRefundApl(order);

            dexService.cancelOffer(order);

            dexService.reopenIncomeOrders(order.getId());
        } catch (AplException.ExecutiveProcessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        return isDuplicate(DEX_CONTRACT, Long.toUnsignedString(attachment.getOrderId()), duplicates, true)
            || isDuplicate(DEX_CANCEL_ORDER, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public String getName() {
        return "CancelOrder";
    }
}
