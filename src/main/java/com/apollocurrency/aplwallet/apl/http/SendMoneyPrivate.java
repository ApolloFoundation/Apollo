/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMoneyPrivate extends CreateTransaction {

    private static class SendMoneyPrivateHolder {
        private static final SendMoneyPrivate INSTANCE = new SendMoneyPrivate();
    }

    public static SendMoneyPrivate getInstance() {
        return SendMoneyPrivateHolder.INSTANCE;
    }

    private SendMoneyPrivate() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "amountATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountATM = ParameterParser.getAmountATM(req);
        Account account = ParameterParser.getSenderAccount(req);
        return createPrivateTransaction(req, account, recipient, amountATM);
    }
}
