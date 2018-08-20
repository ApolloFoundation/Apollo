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

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetShufflingParticipants extends APIServlet.APIRequestHandler {

    private static class GetShufflingParticipantsHolder {
        private static final GetShufflingParticipants INSTANCE = new GetShufflingParticipants();
    }

    public static GetShufflingParticipants getInstance() {
        return GetShufflingParticipantsHolder.INSTANCE;
    }

    private GetShufflingParticipants() {
        super(new APITag[] {APITag.SHUFFLING}, "shuffling");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long shufflingId = ParameterParser.getUnsignedLong(req, "shuffling", true);
        JSONObject response = new JSONObject();
        JSONArray participantsJSONArray = new JSONArray();
        response.put("participants", participantsJSONArray);
        try (DbIterator<ShufflingParticipant> participants = ShufflingParticipant.getParticipants(shufflingId)) {
            for (ShufflingParticipant participant : participants) {
                participantsJSONArray.add(JSONData.participant(participant));
            }
        }
        return response;
    }

}
