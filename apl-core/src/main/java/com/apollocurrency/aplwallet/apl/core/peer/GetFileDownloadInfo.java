/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import javax.inject.Inject;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileDownloadInfo extends PeerRequestHandler{
    @Inject
    DownloadableFilesManager fm;
    
    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileDownloadInfoResponse res = new FileDownloadInfoResponse();
        FileDownloadInfoRequest rq = mapper.convertValue(request, FileDownloadInfoRequest.class);
        res.downloadInfo = fm.getFileDownloadInfo(rq.fileId);
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;        
    }

    @Override
    boolean rejectWhileDownloading() {
       return false;
    }
    
}
