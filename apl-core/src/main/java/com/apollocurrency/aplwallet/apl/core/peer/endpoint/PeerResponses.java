/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * Fixed responses for peer-ro-peer
 * @author alukin@gmail.com
 */
public class PeerResponses {
 
    public static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_REQUEST_TYPE);
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }
    public static final JSONStreamAware CONNECTION_TIMEOUT;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.CONNECTION_TIMEOUT);
        CONNECTION_TIMEOUT = JSON.prepare(response);
    }

    public static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_PROTOCOL);
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    public static final JSONStreamAware UNKNOWN_PEER;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNKNOWN_PEER);
        UNKNOWN_PEER = JSON.prepare(response);
    }

    public static final JSONStreamAware SEQUENCE_ERROR;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.SEQUENCE_ERROR);
        SEQUENCE_ERROR = JSON.prepare(response);
    }
    public static final JSONStreamAware INCORRECT_CHAIN_ID;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.CHAIN_ID_ERROR);
        INCORRECT_CHAIN_ID = JSON.prepare(response);
    }

    public static final JSONStreamAware MAX_INBOUND_CONNECTIONS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.MAX_INBOUND_CONNECTIONS);
        MAX_INBOUND_CONNECTIONS = JSON.prepare(response);
    }

    public static final JSONStreamAware DOWNLOADING;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.DOWNLOADING);
        DOWNLOADING = JSON.prepare(response);
    }

    public static final JSONStreamAware LIGHT_CLIENT;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.LIGHT_CLIENT);
        LIGHT_CLIENT = JSON.prepare(response);
    }

    public static JSONStreamAware error(Exception e) {
        JSONObject response = new JSONObject();
        response.put("error", Peers.hideErrorDetails ? e.getClass().getName() : e.toString());
        return response;
    }

   
}
