/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */
package apl.http;
import apl.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendUpdateTransaction extends CreateTransaction {

    static final SendUpdateTransaction instance = new SendUpdateTransaction();

    private SendUpdateTransaction() {
        super(new APITag[] {APITag.UPDATE, APITag.CREATE_TRANSACTION}, "architecture", "platform", "signature", "hash", "version", "url");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        return createTransaction(req, null, null);
    }
}

