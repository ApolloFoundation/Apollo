/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class SendMoneyPrivate extends CreateTransactionHandler {

    public SendMoneyPrivate() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "amountATM");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long recipient = HttpParameterParserUtil.getAccountId(req, "recipient", true);
        long amountATM = HttpParameterParserUtil.getAmountATM(req);
        Account account = HttpParameterParserUtil.getSenderAccount(req);
        return createPrivateTransaction(req, account, recipient, amountATM);
    }
}
