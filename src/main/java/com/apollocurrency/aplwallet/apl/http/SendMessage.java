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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;

public final class SendMessage extends CreateTransaction {

    private static class SendMessageHolder {
        private static final SendMessage INSTANCE = new SendMessage();
    }

    public static SendMessage getInstance() {
        return SendMessageHolder.INSTANCE;
    }

    private SendMessage() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION}, "recipient");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        Account account = ParameterParser.getSenderAccount(req, validate);
        return new CreateTransactionRequestData(Attachment.ARBITRARY_MESSAGE, recipientId, account, 0);
    }

}
