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
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;


public final class BuyAlias extends CreateTransaction {

    private static class BuyAliasHolder {
        private static final BuyAlias INSTANCE = new BuyAlias();
    }

    public static BuyAlias getInstance() {
        return BuyAliasHolder.INSTANCE;
    }

    private BuyAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName", "amountATM");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Account buyer = ParameterParser.getSenderAccount(req);
        Alias alias = ParameterParser.getAlias(req);
        long amountATM = ParameterParser.getAmountATM(req);
        if (Alias.getOffer(alias) == null) {
            return INCORRECT_ALIAS_NOTFORSALE;
        }
        long sellerId = alias.getAccountId();
        Attachment attachment = new Attachment.MessagingAliasBuy(alias.getAliasName());
        return createTransaction(req, buyer, sellerId, amountATM, attachment);
    }
}
