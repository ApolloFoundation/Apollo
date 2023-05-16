package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class ShardingInfoResponseParser implements JsonReqRespParser<ShardingInfoResponse> {
    @Override
    public ShardingInfoResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, ShardingInfoResponse.class);
    }
}
