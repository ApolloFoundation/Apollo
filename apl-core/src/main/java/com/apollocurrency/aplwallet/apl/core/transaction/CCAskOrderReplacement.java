/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
class CCAskOrderReplacement extends ColoredCoinsOrderPlacement {
    
    public CCAskOrderReplacement() {
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_ASK_ORDER_PLACEMENT;
    }

    @Override
    public String getName() {
        return "AskOrderPlacement";
    }

    @Override
    public ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderPlacement(buffer);
    }

    @Override
    public ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAskOrderPlacement(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceATU(attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        Order.Ask.addOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
        senderAccount.addToUnconfirmedAssetBalanceATU(getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }
    
}
