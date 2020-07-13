/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.impl.BidOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 * @author al
 */
class CCBidOrderCancellation extends ColoredCoinsOrderCancellation {
    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService;

    public CCBidOrderCancellation() {
    }

    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> lookupBidOrderService() {
        if (bidOrderService == null) {
            this.bidOrderService = CDI.current().select(
                BidOrderServiceImpl.class,
                BidOrderService.Literal.INSTANCE
            ).get();
        }
        return bidOrderService;
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
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
        BidOrder order = lookupBidOrderService().getOrder(attachment.getOrderId());
        lookupBidOrderService().removeOrder(attachment.getOrderId());
        if (order != null) {
            lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(order.getQuantityATU(), order.getPriceATM()));
        }
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
        BidOrder bid = lookupBidOrderService().getOrder(attachment.getOrderId());
        if (bid == null) {
            throw new AplException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
        }
        if (bid.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(bid.getAccountId()));
        }
    }

}
