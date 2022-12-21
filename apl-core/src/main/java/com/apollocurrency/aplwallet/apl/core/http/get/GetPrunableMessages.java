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

import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetPrunableMessages extends AbstractAPIRequestHandler {
    private PrunableMessageService prunableMessageService = CDI.current().select(PrunableMessageService.class).get();

    public GetPrunableMessages() {
        super(new APITag[]{APITag.MESSAGES}, "account", "otherAccount", "secretPhrase", "firstIndex", "lastIndex", "timestamp", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = HttpParameterParserUtil.getAccountId(req, true);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        final int timestamp = HttpParameterParserUtil.getTimestamp(req);
        long otherAccountId = HttpParameterParserUtil.getAccountId(req, "otherAccount", false);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("prunableMessages", jsonArray);

        List<PrunableMessage> messages = otherAccountId == 0 ? prunableMessageService.getAll(accountId, firstIndex, lastIndex)
            : prunableMessageService.getAll(accountId, otherAccountId, firstIndex, lastIndex);
        for (PrunableMessage message : messages) {
            if (message.getBlockTimestamp() < timestamp) {
                break;
            }
            jsonArray.add(JSONData.prunableMessage(prunableMessageService, message, keySeed, null));
        }
        return response;
    }

}
