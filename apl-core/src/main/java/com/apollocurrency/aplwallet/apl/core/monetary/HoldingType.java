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

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;

import javax.enterprise.inject.spi.CDI;

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
            HoldingType.lookupAccountService().addToBalanceATM(account, event, eventId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            HoldingType.lookupAccountService().addToUnconfirmedBalanceATM(account, event, eventId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            HoldingType.lookupAccountService().addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, amount);
        }

    },

    ASSET((byte) 1) {
        @Override
        public long getBalance(Account account, long holdingId) {
            return HoldingType.lookupAccountAssetService().getAssetBalanceATU(account, holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return HoldingType.lookupAccountAssetService().getUnconfirmedAssetBalanceATU(account, holdingId);
        }

        @Override
        public void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountAssetService().addToAssetBalanceATU(account, event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountAssetService().addToUnconfirmedAssetBalanceATU(account, event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountAssetService().addToAssetAndUnconfirmedAssetBalanceATU(account, event, eventId, holdingId, amount);
        }

    },

    CURRENCY((byte) 2) {
        @Override
        public long getBalance(Account account, long holdingId) {
            return HoldingType.lookupAccountCurrencyService().getCurrencyUnits(account, holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return HoldingType.lookupAccountCurrencyService().getUnconfirmedCurrencyUnits(account, holdingId);
        }

        @Override
        public void addToBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountCurrencyService().addToCurrencyUnits(account, event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountCurrencyService().addToUnconfirmedCurrencyUnits(account, event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, LedgerEvent event, long eventId, long holdingId, long amount) {
            HoldingType.lookupAccountCurrencyService().addToCurrencyAndUnconfirmedCurrencyUnits(account, event, eventId, holdingId, amount);
        }

    };

    private static AccountService accountService;
    private static AccountAssetService accountAssetService;
    private static AccountCurrencyService accountCurrencyService;
    private final byte code;

    HoldingType(byte code) {
        this.code = code;
    }

    HoldingType() {
        this.code = 0;
    }

    private static AccountService lookupAccountService() {
        if (accountService == null) {
            accountService = CDI.current().select(AccountServiceImpl.class).get();
        }
        return accountService;
    }

    private static AccountAssetService lookupAccountAssetService() {
        if (accountAssetService == null) {
            accountAssetService = CDI.current().select(AccountAssetServiceImpl.class).get();
        }
        return accountAssetService;
    }

    private static AccountCurrencyService lookupAccountCurrencyService() {
        if (accountCurrencyService == null) {
            accountCurrencyService = CDI.current().select(AccountCurrencyServiceImpl.class).get();
        }
        return accountCurrencyService;
    }

    public static HoldingType get(byte code) {
        for (HoldingType holdingType : values()) {
            if (holdingType.getCode() == code) {
                return holdingType;
            }
        }
        throw new IllegalArgumentException("Invalid holdingType code: " + code);
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
