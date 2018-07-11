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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.PhasingVote;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingPollVote extends APIServlet.APIRequestHandler  {
    private static class GetPhasingPollVoteHolder {
        private static final GetPhasingPollVote INSTANCE = new GetPhasingPollVote();
    }

    public static GetPhasingPollVote getInstance() {
        return GetPhasingPollVoteHolder.INSTANCE;
    }

    private GetPhasingPollVote() {
        super(new APITag[] {APITag.PHASING}, "transaction", "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        long accountId = ParameterParser.getAccountId(req, true);

        PhasingVote phasingVote = PhasingVote.getVote(transactionId, accountId);
        if (phasingVote != null) {
            return JSONData.phasingPollVote(phasingVote);
        }
        return JSON.emptyJSON;
    }
}
