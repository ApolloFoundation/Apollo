/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.FileChunkResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class FileChunkResponseParser implements JsonReqRespParser<FileChunkResponse> {
    @Override
    public FileChunkResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, FileChunkResponse.class);
    }

}
