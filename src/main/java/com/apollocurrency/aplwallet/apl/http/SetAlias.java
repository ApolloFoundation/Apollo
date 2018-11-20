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


import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_URI_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_ALIAS_NAME;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Alias;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;

public final class SetAlias extends CreateTransaction {

    private static class SetAliasHolder {
        private static final SetAlias INSTANCE = new SetAlias();
    }

    public static SetAlias getInstance() {
        return SetAliasHolder.INSTANCE;
    }

    private SetAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "aliasName", "aliasURI");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        String aliasURI = Convert.nullToEmpty(req.getParameter("aliasURI"));

        if (aliasName == null) {
            return new CreateTransactionRequestData(MISSING_ALIAS_NAME);
        }

        aliasName = aliasName.trim();
        if (aliasName.length() == 0 || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ALIAS_LENGTH);
        }

        String normalizedAlias = aliasName.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return new CreateTransactionRequestData(INCORRECT_ALIAS_NAME);
            }
        }

        aliasURI = aliasURI.trim();
        if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_URI_LENGTH);
        }

        Account account = ParameterParser.getSenderAccount(req, validate);

        Alias alias = Alias.getAlias(normalizedAlias);
        if (alias != null && validate && alias.getAccountId() != account.getId()) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "\"" + aliasName + "\" is already used");
            return new CreateTransactionRequestData(response);
        }

        Attachment attachment = new Attachment.MessagingAliasAssignment(aliasName, aliasURI);
        return new CreateTransactionRequestData(attachment, account);

    }

}
