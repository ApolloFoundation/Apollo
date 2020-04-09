/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2018 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.util.AplException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.simple.JSONStreamAware;

public final class ParameterException extends AplException {

    private JSONStreamAware errorResponse;
    private JsonNode errorResponseNode;

    public ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ParameterException(JsonNode errorResponseNode) {
        this.errorResponseNode = errorResponseNode;
    }

    public ParameterException(JSONStreamAware errorResponse, JsonNode errorResponseNode) {
        this.errorResponse = errorResponse;
        this.errorResponseNode = errorResponseNode;
    }


    public JSONStreamAware getErrorResponse() {
        return errorResponse;
    }

    public JsonNode getErrorResponseNode() {
        return errorResponseNode;
    }

    @Override
    public String toString() {
        return "ParameterException{" +
            "errorResponse=" + errorResponse +
            ", errorResponseNode=" + errorResponseNode +
            '}';
    }
}
