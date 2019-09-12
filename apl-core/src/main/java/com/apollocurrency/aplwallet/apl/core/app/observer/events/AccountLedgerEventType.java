/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

/**
 * @author andrew.zinchenko@gmail.com
 */
public enum AccountLedgerEventType {
    ADD_ENTRY,
    COMMIT_ENTRIES,
    CLEAR_ENTRIES,
    LOG_ENTRY, LOG_UNCONFIRMED_ENTRY
}
