/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountATM = ParameterParser.getAmountATM(req);
        Account account = ParameterParser.getSenderAccount(req);
        return createPrivateTransaction(req, account, recipient, amountATM);
    }
}
