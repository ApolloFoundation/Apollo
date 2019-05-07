/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;

public abstract class DEX extends TransactionType {

    @Override
    public byte getType() {
        return TransactionType.TYPE_DEX;
    }


    public static final TransactionType DEX_OFFER_TRANSACTION = new DexOfferTransaction();
    public static final TransactionType DEX_CANCEL_OFFER_TRANSACTION = new DexCancelOfferTransaction();

}
