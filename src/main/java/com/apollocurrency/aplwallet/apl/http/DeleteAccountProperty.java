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
import com.apollocurrency.aplwallet.apl.util.Convert;

public final class DeleteAccountProperty extends CreateTransaction {

    private static class DeleteAccountPropertyHolder {
        private static final DeleteAccountProperty INSTANCE = new DeleteAccountProperty();
    }

    public static DeleteAccountProperty getInstance() {
        return DeleteAccountPropertyHolder.INSTANCE;
    }

    private DeleteAccountProperty() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "property", "setter");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {

        Account senderAccount = ParameterParser.getSenderAccount(req, validate);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        if (recipientId == 0 && validate) {
            recipientId = senderAccount.getId();
        }
        long setterId = ParameterParser.getAccountId(req, "setter", false);
        if (setterId == 0 && validate) {
            setterId = senderAccount.getId();
        }
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        if (property.isEmpty()) {
            return new CreateTransactionRequestData(JSONResponses.MISSING_PROPERTY);
        }
        Account.AccountProperty accountProperty = Account.getProperty(recipientId, property, setterId);
        if (accountProperty == null && validate) {
            return new CreateTransactionRequestData(JSONResponses.UNKNOWN_PROPERTY);
        }
        if (validate && accountProperty.getRecipientId() != senderAccount.getId() && accountProperty.getSetterId() != senderAccount.getId()) {
            return new CreateTransactionRequestData(JSONResponses.INCORRECT_PROPERTY);
        }
        Attachment attachment = new Attachment.MessagingAccountPropertyDelete(accountProperty == null ? 0 : accountProperty.getId());
        return new CreateTransactionRequestData(attachment, recipientId,senderAccount, 0);

    }

}
