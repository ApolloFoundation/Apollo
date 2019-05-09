/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.FileChunkResonse;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileChunk extends PeerRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GetFileChunk.class);

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileChunkResonse res = new FileChunkResonse();

        FileChunkRequest fcr = mapper.convertValue(request, FileChunkRequest.class);
        try {
            ChunkedFileOps ops = new ChunkedFileOps(fcr.fileId);
            byte[] dataBuf = new byte[fcr.size];
            int rres = ops.readChunk(fcr.offset, fcr.size, dataBuf);
            if (rres != fcr.size) {
                res.errorCode = -1;
            }
            FileChunk fc = new FileChunk();
            res.chunk = fc;
        } catch (IOException ex) {
            LOG.error("Error reading file with id: " + fcr.fileId, ex);
            res.errorCode = -2;
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;

    }

    @Override
    boolean rejectWhileDownloading() {
        return false;
    }

}
