/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileDownloadInfo extends PeerRequestHandler{

    private DownloadableFilesManager downloadableFilesManager;

    public GetFileDownloadInfo(DownloadableFilesManager downloadableFilesManager) {
        this.downloadableFilesManager = downloadableFilesManager;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileDownloadInfoResponse res = new FileDownloadInfoResponse();
        FileDownloadInfoRequest rq = mapper.convertValue(request, FileDownloadInfoRequest.class);
        res.downloadInfo = downloadableFilesManager.getFileDownloadInfo(rq.fileId);
        if(res.downloadInfo==null || !res.downloadInfo.fileInfo.isPresent){
            res.errorCode=-2;
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;        
    }

    @Override
    public boolean rejectWhileDownloading() {
       return false;
    }
    
}
