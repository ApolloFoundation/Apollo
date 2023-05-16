package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class FileDownloadInfoResponseParser implements JsonReqRespParser<FileDownloadInfoResponse> {
    @Override
    public FileDownloadInfoResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, FileDownloadInfoResponse.class);
    }
}
