/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileInfoResponse;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import javax.inject.Inject;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileInfo extends PeerRequestHandler{
    @Inject
    DownloadableFilesManager fm;
    
    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileInfoResponse res = new FileInfoResponse();
        FileDownloadInfoRequest rq = mapper.convertValue(request, FileDownloadInfoRequest.class);
        res.fileInfo = fm.getFileDownloadInfo(rq.fileId).fileInfo;
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response; 
    }

    @Override
    public boolean rejectWhileDownloading() {
       return false;
    }
    
}
