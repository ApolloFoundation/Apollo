/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountLedgerService {

    int getTrimKeep();

    boolean mustLogEntry(long accountId, boolean isUnconfirmed);

    void logEntry(LedgerEntry ledgerEntry);

    void commitEntries();

    void clearEntries();

    LedgerEntry getEntry(long ledgerId, boolean allowPrivate);

    List<LedgerEntry> getEntries(long accountId, LedgerEvent event, long eventId,
                                 LedgerHolding holding, long holdingId,
                                 int firstIndex, int lastIndex, boolean includePrivate);
}
