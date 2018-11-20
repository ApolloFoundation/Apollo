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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ACCOUNT_NAME_LENGTH;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;

public final class SetAccountInfo extends CreateTransaction {

    private static class SetAccountInfoHolder {
        private static final SetAccountInfo INSTANCE = new SetAccountInfo();
    }

    public static SetAccountInfo getInstance() {
        return SetAccountInfoHolder.INSTANCE;
    }

    private SetAccountInfo() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "name", "description");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        String name = Convert.nullToEmpty(req.getParameter("name")).trim();
        String description = Convert.nullToEmpty(req.getParameter("description")).trim();

        if (name.length() > Constants.MAX_ACCOUNT_NAME_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ACCOUNT_NAME_LENGTH);
        }

        if (description.length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ACCOUNT_DESCRIPTION_LENGTH);
        }

        Account account = ParameterParser.getSenderAccount(req, validate);
        Attachment attachment = new Attachment.MessagingAccountInfo(name, description);
        return new CreateTransactionRequestData(attachment, account);

    }

}
