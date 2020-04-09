/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.inject.Inject;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class GetFileDownloadInfo extends PeerRequestHandler {

    private DownloadableFilesManager downloadableFilesManager;

    @Inject
    public GetFileDownloadInfo(DownloadableFilesManager downloadableFilesManager) {
        this.downloadableFilesManager = downloadableFilesManager;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileDownloadInfoResponse res = new FileDownloadInfoResponse();
        FileDownloadInfoRequest rq = mapper.convertValue(request, FileDownloadInfoRequest.class);
        log.debug("GetFileDownloadInfo request = {}", rq);

        res.downloadInfo = downloadableFilesManager.getFileDownloadInfo(rq.fileId);
        if (res.downloadInfo == null || !res.downloadInfo.fileInfo.isPresent) {
            res.errorCode = -2;
            log.debug("FileID: {} is not present", rq.fileId);
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        log.debug("GetFileDownloadInfo response = {}", response);
        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
