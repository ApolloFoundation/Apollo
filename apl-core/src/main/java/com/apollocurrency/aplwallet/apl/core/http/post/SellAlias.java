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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ALIAS_OWNER;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_RECIPIENT;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;


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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Alias alias = ParameterParser.getAlias(req);
        Account owner = ParameterParser.getSenderAccount(req);

        long priceATM =
                ParameterParser.getLong(req, "priceATM", 0L, CDI.current().select(BlockchainConfig.class).get().getCurrentConfig().getMaxBalanceATM(),
                true);

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

        Attachment attachment = new MessagingAliasSell(alias.getAliasName(), priceATM);
        return createTransaction(req, owner, recipientId, 0, attachment);
    }
}
