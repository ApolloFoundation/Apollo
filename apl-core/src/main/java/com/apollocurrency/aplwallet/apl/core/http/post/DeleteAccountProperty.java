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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAccountPropertyDelete;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account senderAccount = ParameterParser.getSenderAccount(req);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        if (recipientId == 0) {
            recipientId = senderAccount.getId();
        }
        long setterId = ParameterParser.getAccountId(req, "setter", false);
        if (setterId == 0) {
            setterId = senderAccount.getId();
        }
        String property = Convert.nullToEmpty(req.getParameter("property")).trim();
        if (property.isEmpty()) {
            return JSONResponses.MISSING_PROPERTY;
        }
        AccountProperty accountProperty = AccountPropertyTable.getProperty(recipientId, property, setterId);
        if (accountProperty == null) {
            return JSONResponses.UNKNOWN_PROPERTY;
        }
        if (accountProperty.getRecipientId() != senderAccount.getId() && accountProperty.getSetterId() != senderAccount.getId()) {
            return JSONResponses.INCORRECT_PROPERTY;
        }
        Attachment attachment = new MessagingAccountPropertyDelete(accountProperty.getId());
        return createTransaction(req, senderAccount, recipientId, 0, attachment);

    }

}
