/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.respons.BaseP2PResponse;
import org.json.simple.JSONObject;

public interface PeerResponseParser<T extends BaseP2PResponse> {

    T parse(JSONObject json);

}
