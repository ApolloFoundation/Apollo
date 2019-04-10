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
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetPhasingPolls extends AbstractAPIRequestHandler {

    public GetPhasingPolls() {
        super(new APITag[] {APITag.PHASING}, "transaction", "transaction", "transaction", "countVotes"); // limit to 3 for testing
    }
    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] transactionIds = ParameterParser.getUnsignedLongs(req, "transaction");
        boolean countVotes = "true".equalsIgnoreCase(req.getParameter("countVotes"));
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("polls", jsonArray);
        for (long transactionId : transactionIds) {
            PhasingPoll poll = phasingPollService.getPoll(transactionId);
            if (poll != null) {
                jsonArray.add(JSONData.phasingPoll(poll, countVotes));
            } else {
                PhasingPollResult pollResult = phasingPollService.getResult(transactionId);
                if (pollResult != null) {
                    jsonArray.add(JSONData.phasingPollResult(pollResult));
                }
            }
        }
        return response;
    }
}
