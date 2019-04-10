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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemReserveClaim;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Claim currency units and receive back APL invested into this currency before it became active
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>units - the number of currency units claimed<br>
 * This value is multiplied by current currency rate and the result is added to the sender APL account balance.
 * </ul>
 * <p>
 * Constraints
 * <p>This transaction is allowed only when the currency is {@link com.apollocurrency.aplwallet.apl.CurrencyType#CLAIMABLE} and is already active.<br>
 */
@Vetoed
public final class CurrencyReserveClaim extends CreateTransaction {

    public CurrencyReserveClaim() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "units");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, currency.getReserveSupply(), false);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MonetarySystemReserveClaim(currency.getId(), units);
        return createTransaction(req, account, attachment);

    }

}
