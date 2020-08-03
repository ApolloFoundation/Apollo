/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class CCBidOrderCancellationTransactionType extends ColoredCoinsOrderCancellationTransactionType {
    private final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService;

    @Inject
    public CCBidOrderCancellationTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService) {
        super(blockchainConfig, accountService);
        this.bidOrderService = bidOrderService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_CANCELLATION;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_BID_ORDER_CANCELLATION;
    }

    @Override
    public String getName() {
        return "BidOrderCancellation";
    }

    @Override
    public ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsBidOrderCancellation(buffer);
    }

    @Override
    public ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsBidOrderCancellation(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
        BidOrder order = bidOrderService.getOrder(attachment.getOrderId());
        bidOrderService.removeOrder(attachment.getOrderId());
        if (order != null) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(order.getQuantityATU(), order.getPriceATM()));
        }
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
        BidOrder bid = bidOrderService.getOrder(attachment.getOrderId());
        if (bid == null) {
            throw new AplException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
        }
        if (bid.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(bid.getAccountId()));
        }
    }

}
