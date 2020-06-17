/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;

public interface CurrencyMintService {

    void mintCurrency(LedgerEvent event, long eventId, Account account,
                      MonetarySystemCurrencyMinting attachment);

    long getCounter(long currencyId, long accountId);

    void deleteCurrency(Currency currency);

}
