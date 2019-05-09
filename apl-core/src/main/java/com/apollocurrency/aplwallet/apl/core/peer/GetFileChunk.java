/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.FileChunkResonse;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileChunk extends PeerRequestHandler{

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        FileChunkRequest fcr = mapper.convertValue(request, FileChunkRequest.class);
        ChunkedFileOps ops = new ChunkedFileOps(fcr.fileId);
        

        FileChunk fc = new FileChunk();
        FileChunkResonse res = new FileChunkResonse();
        res.chunk=fc;
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response; 
    }

    @Override
    boolean rejectWhileDownloading() {
       return false;
    }
    
}
