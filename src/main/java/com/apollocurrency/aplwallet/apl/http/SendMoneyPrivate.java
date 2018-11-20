/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;

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
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountATM = ParameterParser.getAmountATM(req);
        Account account = ParameterParser.getSenderAccount(req, validate);
        return new CreateTransactionRequestData(Attachment.PRIVATE_PAYMENT, recipient, account, amountATM);
    }
}
