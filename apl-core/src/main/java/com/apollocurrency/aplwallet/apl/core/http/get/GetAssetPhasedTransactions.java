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

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public class GetAssetPhasedTransactions extends AbstractAPIRequestHandler {

    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();

    public GetAssetPhasedTransactions() {
        super(new APITag[]{APITag.AE, APITag.PHASING}, "asset", "account", "withoutWhitelist", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long assetId = HttpParameterParserUtil.getUnsignedLong(req, "asset", true);
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        boolean withoutWhitelist = "true".equalsIgnoreCase(req.getParameter("withoutWhitelist"));

        JSONArray transactions = new JSONArray();
        List<Transaction> holdingPhasedTransactions = phasingPollService.getHoldingPhasedTransactions(assetId, VoteWeighting.VotingModel.ASSET,
            accountId, withoutWhitelist, firstIndex, lastIndex);
        holdingPhasedTransactions.forEach(e-> {
            transactions.add(JSONData.transaction(false, e));
        });
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;
    }

}
