/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

/**
 * @author al
 */
class CCBidOrderPlacement extends ColoredCoinsOrderPlacement {
    private OrderMatchService orderMatchService;

    public CCBidOrderPlacement() {
    }

    private OrderMatchService lookupOrderMatchService() {
        if (orderMatchService == null) {
            this.orderMatchService = CDI.current().select(OrderMatchService.class).get();
        }
        return orderMatchService;
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_BID_ORDER_PLACEMENT;
    }

    @Override
    public String getName() {
        return "BidOrderPlacement";
    }

    @Override
    public ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsBidOrderPlacement(buffer);
    }

    @Override
    public ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsBidOrderPlacement(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM())) {
            lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
        lookupOrderMatchService().addBidOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
        lookupAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
    }

}
