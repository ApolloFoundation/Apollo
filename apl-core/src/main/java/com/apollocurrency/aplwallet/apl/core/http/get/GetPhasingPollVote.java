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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.PhasingVote;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingPollVote extends AbstractAPIRequestHandler {
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
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        long accountId = ParameterParser.getAccountId(req, true);

        PhasingVote phasingVote = PhasingVote.getVote(transactionId, accountId);
        if (phasingVote != null) {
            return JSONData.phasingPollVote(phasingVote);
        }
        return JSON.emptyJSON;
    }
}
