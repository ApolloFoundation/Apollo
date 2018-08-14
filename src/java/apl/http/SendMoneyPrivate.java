/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.http;

import apl.Account;
import apl.AplException;
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
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "amountATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountATM = ParameterParser.getAmountATM(req);
        Account account = ParameterParser.getSenderAccount(req);
        return createPrivateTransaction(req, account, recipient, amountATM);
    }
}
