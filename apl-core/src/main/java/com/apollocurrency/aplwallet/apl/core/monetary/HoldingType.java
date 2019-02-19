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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;

public enum HoldingType {

    APL((byte) 0) {
        @Override
        public long getBalance(Account account, long holdingId) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            return account.getBalanceATM();
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            return account.getUnconfirmedBalanceATM();
        }

        @Override
        public void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToBalanceATM(event, eventId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToUnconfirmedBalanceATM(event, eventId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToBalanceAndUnconfirmedBalanceATM(event, eventId, amount);
        }

    },

    ASSET((byte) 1) {
        @Override
        public long getBalance(Account account, long holdingId) {
            return account.getAssetBalanceATU(holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return account.getUnconfirmedAssetBalanceATU(holdingId);
        }

        @Override
        public void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToAssetBalanceATU(event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToUnconfirmedAssetBalanceATU(event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToAssetAndUnconfirmedAssetBalanceATU(event, eventId, holdingId, amount);
        }

    },

    CURRENCY((byte) 2) {
        @Override
        public long getBalance(Account account, long holdingId) {
            return account.getCurrencyUnits(holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return account.getUnconfirmedCurrencyUnits(holdingId);
        }

        @Override
        public void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToCurrencyUnits(event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToUnconfirmedCurrencyUnits(event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToCurrencyAndUnconfirmedCurrencyUnits(event, eventId, holdingId, amount);
        }

    };

    public static HoldingType get(byte code) {
        for (HoldingType holdingType : values()) {
            if (holdingType.getCode() == code) {
                return holdingType;
            }
        }
        throw new IllegalArgumentException("Invalid holdingType code: " + code);
    }

    private final byte code;

    HoldingType(byte code) {
        this.code = code;
    }

    HoldingType() {
        this.code = 0;
    }

    public byte getCode() {
        return code;
    }

    public abstract long getBalance(Account account, long holdingId);

    public abstract long getUnconfirmedBalance(Account account, long holdingId);

    public abstract void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount);

    public abstract void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount);

    public abstract void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount);

}
