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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NOT_ENOUGH_CURRENCY;

public final class TransferCurrency extends CreateTransaction {

    private static class TransferCurrencyHolder {
        private static final TransferCurrency INSTANCE = new TransferCurrency();
    }

    public static TransferCurrency getInstance() {
        return TransferCurrencyHolder.INSTANCE;
    }

    private TransferCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "recipient", "currency", "units");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long recipient = ParameterParser.getAccountId(req, "recipient", true);

        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new MonetarySystemCurrencyTransfer(currency.getId(), units);
        try {
            return createTransaction(req, account, recipient, 0, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return NOT_ENOUGH_CURRENCY;
        }
    }

}
