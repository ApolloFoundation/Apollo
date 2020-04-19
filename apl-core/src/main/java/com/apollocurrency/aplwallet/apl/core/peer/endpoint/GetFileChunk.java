/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.FileChunkResponse;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class GetFileChunk extends PeerRequestHandler {
    private DownloadableFilesManager downloadableFilesManager;

    @Inject
    public GetFileChunk(DownloadableFilesManager downloadableFilesManager) {
        this.downloadableFilesManager = downloadableFilesManager;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileChunkResponse res = new FileChunkResponse();

        FileChunkRequest fcr = mapper.convertValue(request, FileChunkRequest.class);
        log.debug("FileChunkReq = {}", fcr);
        Path filePath = downloadableFilesManager.mapFileIdToLocalPath(fcr.fileId);
        try {
            ChunkedFileOps ops = new ChunkedFileOps(filePath.toAbsolutePath());
            byte[] dataBuf = new byte[fcr.size.intValue()];
            Integer rres = ops.readChunk(fcr.offset, fcr.size, dataBuf);
            if (rres != fcr.size.intValue()) {
                res.errorCode = -1;
            }
            FileChunk fc = new FileChunk();
            fc.info.crc = ops.getLastRDChunkCrc();
            fc.info.fileId = fcr.fileId;
            fc.info.size = rres.longValue();
            fc.info.offset = fcr.offset;
            fc.mime64data = Base64.getEncoder().encodeToString(dataBuf);
            res.chunk = fc;
        } catch (IOException ex) {
            log.error("Error reading file with id: " + fcr.fileId, ex);
            res.errorCode = -2;
        }
        log.trace("FileChunkResponse = {}", res.chunk.info);
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;

    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
