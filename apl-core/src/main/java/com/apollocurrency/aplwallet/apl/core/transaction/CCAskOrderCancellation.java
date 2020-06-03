/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.impl.AskOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderCancellation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 * @author al
 */
class CCAskOrderCancellation extends ColoredCoinsOrderCancellation {
    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService;

    public CCAskOrderCancellation() {
    }

    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> lookupAskOrderService() {
        if (askOrderService == null) {
            this.askOrderService = CDI.current().select(
                AskOrderServiceImpl.class,
                AskOrderService.Literal.INSTANCE
            ).get();
        }
        return askOrderService;
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_ASK_ORDER_CANCELLATION;
    }

    @Override
    public String getName() {
        return "AskOrderCancellation";
    }

    @Override
    public ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderCancellation(buffer);
    }

    @Override
    public ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderCancellation(attachmentData);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
        AskOrder order = lookupAskOrderService().getOrder(attachment.getOrderId());
        lookupAskOrderService().removeOrder(attachment.getOrderId());
        if (order != null) {
            lookupAccountAssetService().addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), order.getAssetId(), order.getQuantityATU());
        }
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
        AskOrder ask = lookupAskOrderService().getOrder(attachment.getOrderId());
        if (ask == null) {
            throw new AplException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
        }
        if (ask.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account " + Long.toUnsignedString(ask.getAccountId()));
        }
    }

}
