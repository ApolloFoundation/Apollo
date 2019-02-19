/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemAttachment;
import java.util.Map;

public abstract class MonetarySystem extends TransactionType {

    public  static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE = 0;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE = 1;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM = 2;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER = 3;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER = 4;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY = 5;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL = 6;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING = 7;
    public  static final byte SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION = 8;

    public static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_ISSUANCE:
                return MonetarySystem.CURRENCY_ISSUANCE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_INCREASE:
                return MonetarySystem.RESERVE_INCREASE;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_RESERVE_CLAIM:
                return MonetarySystem.RESERVE_CLAIM;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_TRANSFER:
                return MonetarySystem.CURRENCY_TRANSFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_PUBLISH_EXCHANGE_OFFER:
                return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_BUY:
                return MonetarySystem.EXCHANGE_BUY;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_EXCHANGE_SELL:
                return MonetarySystem.EXCHANGE_SELL;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_MINTING:
                return MonetarySystem.CURRENCY_MINTING;
            case MonetarySystem.SUBTYPE_MONETARY_SYSTEM_CURRENCY_DELETION:
                return MonetarySystem.CURRENCY_DELETION;
            default:
                return null;
        }
    }

    public MonetarySystem() {}

    @Override
    public final byte getType() {
        return TransactionType.TYPE_MONETARY_SYSTEM;
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        MonetarySystemAttachment attachment = (MonetarySystemAttachment) transaction.getAttachment();
        Currency currency = Currency.getCurrency(attachment.getCurrencyId());
        String nameLower = currency.getName().toLowerCase();
        String codeLower = currency.getCode().toLowerCase();
        boolean isDuplicate = TransactionType.isDuplicate(CURRENCY_ISSUANCE, nameLower, duplicates, false);
        if (! nameLower.equals(codeLower)) {
            isDuplicate = isDuplicate || TransactionType.isDuplicate(CURRENCY_ISSUANCE, codeLower, duplicates, false);
        }
        return isDuplicate;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    public static final TransactionType CURRENCY_ISSUANCE = new MonetarySystemCurrIssuance();

    public static final TransactionType RESERVE_INCREASE = new MSReverseIncrease();

    public static final TransactionType RESERVE_CLAIM = new MSReverseClaim();

    public static final TransactionType CURRENCY_TRANSFER = new MSCurrencyTransfer();

    public static final TransactionType PUBLISH_EXCHANGE_OFFER = new MSPublishExchangeOffer();


    public static final TransactionType EXCHANGE_BUY = new MSExchangeBuy();

    public static final TransactionType EXCHANGE_SELL = new MSExchangeSell();

    public static final TransactionType CURRENCY_MINTING = new MSCurrencyMinting();

    public static final TransactionType CURRENCY_DELETION = new MSCurrencyDeletion();


}
