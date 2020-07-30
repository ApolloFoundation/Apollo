/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.respons.GetMilestoneBlockIdsResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class GetMilestoneBlockIdsResponseParser implements PeerResponseParser<GetMilestoneBlockIdsResponse> {
    @Override
    public GetMilestoneBlockIdsResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, GetMilestoneBlockIdsResponse.class);
    }
}
