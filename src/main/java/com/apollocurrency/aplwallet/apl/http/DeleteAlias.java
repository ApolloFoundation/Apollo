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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ALIAS_OWNER;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Alias;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;


public final class DeleteAlias extends CreateTransaction {

    private static class DeleteAliasHolder {
        private static final DeleteAlias INSTANCE = new DeleteAlias();
    }

    public static DeleteAlias getInstance() {
        return DeleteAliasHolder.INSTANCE;
    }

    private DeleteAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(final HttpServletRequest req, boolean validate) throws AplException {
        final Alias alias = ParameterParser.getAlias(req);
        final Account owner = ParameterParser.getSenderAccount(req, validate);

        if (validate && alias.getAccountId() != owner.getId()) {
            return new CreateTransactionRequestData(INCORRECT_ALIAS_OWNER);
        }

        final Attachment attachment = new Attachment.MessagingAliasDelete(alias.getAliasName());
        return new CreateTransactionRequestData(attachment, owner);
    }
}
