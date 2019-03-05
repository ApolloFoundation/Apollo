/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderCancellationAttachment;
import java.util.Map;

/**
 *
 * @author al
 */
public abstract class ColoredCoinsOrderCancellation extends ColoredCoins {
    
    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        ColoredCoinsOrderCancellationAttachment attachment = (ColoredCoinsOrderCancellationAttachment) transaction.getAttachment();
        return TransactionType.isDuplicate(ColoredCoins.ASK_ORDER_CANCELLATION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return true;
    }
    
}
