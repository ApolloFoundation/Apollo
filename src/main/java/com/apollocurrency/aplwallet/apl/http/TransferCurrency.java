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

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.NOT_ENOUGH_CURRENCY;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Currency;

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
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

        long recipient = ParameterParser.getAccountId(req, "recipient", true);

        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemCurrencyTransfer(currency.getId(), units);
        return new CreateTransactionRequestData(attachment, recipient, account, 0, NOT_ENOUGH_CURRENCY);
    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        return new CreateTransactionRequestData(new Attachment.MonetarySystemCurrencyTransfer(0, 0), null);
    }
}
