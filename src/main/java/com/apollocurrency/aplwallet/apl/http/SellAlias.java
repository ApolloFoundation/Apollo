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

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Alias;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ALIAS_OWNER;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_RECIPIENT;


public final class SellAlias extends CreateTransaction {

    private static class SellAliasHolder {
        private static final SellAlias INSTANCE = new SellAlias();
    }

    public static SellAlias getInstance() {
        return SellAliasHolder.INSTANCE;
    }

    private SellAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName", "recipient", "priceATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Alias alias = ParameterParser.getAlias(req);
        Account owner = ParameterParser.getSenderAccount(req);

        long priceATM = ParameterParser.getLong(req, "priceATM", 0L, Constants.MAX_BALANCE_ATM, true);

        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        long recipientId = 0;
        if (recipientValue != null) {
            try {
                recipientId = Convert.parseAccountId(recipientValue);
            } catch (RuntimeException e) {
                return INCORRECT_RECIPIENT;
            }
            if (recipientId == 0) {
                return INCORRECT_RECIPIENT;
            }
        }

        if (alias.getAccountId() != owner.getId()) {
            return INCORRECT_ALIAS_OWNER;
        }

        Attachment attachment = new Attachment.MessagingAliasSell(alias.getAliasName(), priceATM);
        return createTransaction(req, owner, recipientId, 0, attachment);
    }
}
