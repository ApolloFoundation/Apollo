/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.apl.core.peer.respons.PeerResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.simple.JSONObject;

public interface PeerResponseParser<T extends PeerResponse> {

    T parse(JSONObject json) throws JsonProcessingException;

}
