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

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetAccountProperties extends AbstractAPIRequestHandler {

    public GetAccountProperties() {
        super(new APITag[]{APITag.ACCOUNTS}, "recipient", "property", "setter", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long recipientId = HttpParameterParserUtil.getAccountId(req, "recipient", false);
        long setterId = HttpParameterParserUtil.getAccountId(req, "setter", false);
        if (recipientId == 0 && setterId == 0) {
            return JSONResponses.missing("recipient", "setter");
        }
        String property = Convert.emptyToNull(req.getParameter("property"));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray propertiesJSON = new JSONArray();
        response.put("properties", propertiesJSON);
        if (recipientId != 0) {
            JSONData.putAccount(response, "recipient", recipientId);
        }
        if (setterId != 0) {
            JSONData.putAccount(response, "setter", setterId);
        }
        List<AccountProperty> properties = lookupAccountPropertyService().getProperties(recipientId, setterId, property, firstIndex, lastIndex);
        properties.forEach(accountProperty -> propertiesJSON.add(JSONData.accountProperty(accountProperty, recipientId == 0, setterId == 0)));
        return response;

    }

}
