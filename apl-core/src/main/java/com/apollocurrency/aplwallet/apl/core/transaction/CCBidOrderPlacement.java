/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class CCBidOrderPlacement extends ColoredCoinsOrderPlacement {
    
    public CCBidOrderPlacement() {
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
            senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
        Order.Bid.addOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getQuantityATU(), attachment.getPriceATM()));
    }
    
}
