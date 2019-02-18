/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderCancellation;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class CCBidOrderCancellation extends ColoredCoinsOrderCancellation {
    
    public CCBidOrderCancellation() {
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
        Order order = Order.Bid.getBidOrder(attachment.getOrderId());
        Order.Bid.removeOrder(attachment.getOrderId());
        if (order != null) {
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(order.getQuantityATU(), order.getPriceATM()));
        }
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
        Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
        if (bid == null) {
            throw new AplException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
        }
        if (bid.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(bid.getAccountId()));
        }
    }
    
}
