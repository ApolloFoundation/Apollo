/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetFileInfo extends PeerRequestHandler{

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    boolean rejectWhileDownloading() {
       return false;
    }
    
}
