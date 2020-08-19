package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.respons.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class FileDownloadInfoResponseParser implements ReqRespParser<FileDownloadInfoResponse> {
    @Override
    public FileDownloadInfoResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, FileDownloadInfoResponse.class);
    }
}
