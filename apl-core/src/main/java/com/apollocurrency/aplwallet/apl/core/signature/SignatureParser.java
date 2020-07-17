/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface SignatureParser {
    String SIGNATURE_FIELD_NAME = "signature";
    /**
     * Parse the byte array and build the signature object
     *
     * @param buffer input data array
     * @return the signature object
     */
    Signature parse(ByteBuffer buffer);

    int calcDataSize(int count);

    byte[] bytes(Signature signature);

    /**
     * Parse the JSON object and build the signature object
     *
     * @param json input JSONObject
     * @return the signature object
     */
    Signature parse(JSONObject json);

    /**
     * Return the json representation of the signature object
     *
     * @param signature
     * @return
     */
    JSONObject getJsonObject(Signature signature);

    default String getJsonString(Signature signature) {
        return getJsonObject(signature).toJSONString();
    }

}
