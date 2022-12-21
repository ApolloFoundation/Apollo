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


import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

@Vetoed
public class GetPolls extends AbstractAPIRequestHandler {

    public GetPolls() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.VS}, "account", "firstIndex", "lastIndex", "timestamp", "includeFinished", "finishedOnly");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = HttpParameterParserUtil.getAccountId(req, "account", false);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        boolean finishedOnly = "true".equalsIgnoreCase(req.getParameter("finishedOnly"));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        final int timestamp = HttpParameterParserUtil.getTimestamp(req);

        Stream<Poll> polls;
        if (accountId == 0) {
            if (finishedOnly) {
                polls = pollService.getPollsFinishingAtOrBefore(lookupBlockchain().getHeight(), firstIndex, lastIndex);
            } else if (includeFinished) {
                polls = pollService.getAllPolls(firstIndex, lastIndex);
            } else {
                polls = pollService.getActivePolls(firstIndex, lastIndex);
            }
        } else {
            polls = pollService.getPollsByAccount(accountId, includeFinished, finishedOnly, firstIndex, lastIndex);
        }

        JSONArray pollsJson = polls.takeWhile(poll -> poll.getTimestamp() >= timestamp)
            .map(JSONData::poll)
            .collect(CollectorUtils.jsonCollector());

        JSONObject response = new JSONObject();
        response.put("polls", pollsJson);
        return response;
    }
}
