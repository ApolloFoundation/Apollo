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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ACCOUNT_PROPERTY_NAME_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ACCOUNT_PROPERTY_VALUE_LENGTH;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;

public final class SetAccountProperty extends CreateTransaction {

    private static class SetAccountPropertyHolder {
        private static final SetAccountProperty INSTANCE = new SetAccountProperty();
    }

    public static SetAccountProperty getInstance() {
        return SetAccountPropertyHolder.INSTANCE;
    }

    private SetAccountProperty() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "property", "value", "valueLength");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

        Account senderAccount = ParameterParser.getSenderAccount(req);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        if (recipientId == 0) {
            recipientId = senderAccount.getId();
        }
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        String value = Convert.nullToEmpty(req.getParameter("value")).trim();

        if (property.length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH || property.length() == 0) {
            return new CreateTransactionRequestData(INCORRECT_ACCOUNT_PROPERTY_NAME_LENGTH);
        }

        if (value.length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ACCOUNT_PROPERTY_VALUE_LENGTH);
        }

        Attachment attachment = new Attachment.MessagingAccountProperty(property, value);
        return new CreateTransactionRequestData(attachment, recipientId, senderAccount, 0);

    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        int valueLength = ParameterParser.getInt(req, "valueLength", 0, Integer.MAX_VALUE, false, -1);
        if (valueLength == -1) {
            String value = Convert.nullToEmpty(req.getParameter("value")).trim();
            valueLength = value.length();
        }
        if (valueLength > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ACCOUNT_PROPERTY_VALUE_LENGTH);
        }
        return super.parseFeeCalculationRequest(req);
    }
}
