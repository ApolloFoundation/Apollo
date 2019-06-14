/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

/**
 *
 * @author al
 */
public abstract class ColoredCoins extends TransactionType {
    
    public ColoredCoins() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_COLORED_COINS;
    }
    public static final TransactionType ASSET_ISSUANCE = new CCAssetIssuance();
    public static final TransactionType ASSET_TRANSFER = new CCAssetTransfer();
    public static final TransactionType ASSET_DELETE = new CCAssetDelete();

    public static final TransactionType ASK_ORDER_PLACEMENT = new CCAskOrderReplacement();
    public static final TransactionType BID_ORDER_PLACEMENT = new CCBidOrderPlacement();

    public static final TransactionType ASK_ORDER_CANCELLATION = new CCAskOrderCancellation();
    public static final TransactionType BID_ORDER_CANCELLATION = new CCBidOrderCancellation();
    public static final TransactionType DIVIDEND_PAYMENT = new CCCoinsDividentPayment();
  
}
