/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerHolding;

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
