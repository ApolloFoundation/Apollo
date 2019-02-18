/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import java.util.HashMap;
import java.util.Map;

/**
 * Ledger holdings
 *
 * When adding a new holding, do not change the existing code assignments since
 * they are stored in the holding_type field of the account_ledger table.
 */
public enum LedgerHolding {
    UNCONFIRMED_APL_BALANCE(1, true), APL_BALANCE(2, false), UNCONFIRMED_ASSET_BALANCE(3, true), ASSET_BALANCE(4, false), UNCONFIRMED_CURRENCY_BALANCE(5, true), CURRENCY_BALANCE(6, false);
    /** Holding code mapping */
    private static final Map<Integer, LedgerHolding> holdingMap = new HashMap<>();
    static {
        for (LedgerHolding holding : values()) {
            if (holdingMap.put(holding.code, holding) != null) {
                throw new RuntimeException("LedgerHolding code " + holding.code + " reused");
            }
        }
    }
    /** Holding code */
    private final int code;
    /** Unconfirmed holding */
    private final boolean isUnconfirmed;

    /**
     * Create the holding event
     *
     * @param   code                    Holding code
     * @param   isUnconfirmed           TRUE if the holding is unconfirmed
     */
    LedgerHolding(int code, boolean isUnconfirmed) {
        this.code = code;
        this.isUnconfirmed = isUnconfirmed;
    }

    /**
     * Check if the holding is unconfirmed
     *
     * @return                          TRUE if the holding is unconfirmed
     */
    public boolean isUnconfirmed() {
        return this.isUnconfirmed;
    }

    /**
     * Return the holding code
     *
     * @return                          Holding code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the holding from the holding code
     *
     * @param   code                    Holding code
     * @return                          Holding
     */
    public static LedgerHolding fromCode(int code) {
        LedgerHolding holding = holdingMap.get(code);
        if (holding == null) {
            throw new IllegalArgumentException("LedgerHolding code " + code + " is unknown");
        }
        return holding;
    }
    
}
