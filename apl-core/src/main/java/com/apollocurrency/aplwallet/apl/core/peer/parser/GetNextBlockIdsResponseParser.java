/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.GetNextBlockIdsResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

@EqualsAndHashCode
public class GetNextBlockIdsResponseParser implements JsonReqRespParser<GetNextBlockIdsResponse> {
    @Override
    public GetNextBlockIdsResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, GetNextBlockIdsResponse.class);
    }
}
