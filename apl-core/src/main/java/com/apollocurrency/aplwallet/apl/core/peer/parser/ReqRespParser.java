/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import org.json.simple.JSONObject;

public interface ReqRespParser<T> {
    T parse(JSONObject json);
}
