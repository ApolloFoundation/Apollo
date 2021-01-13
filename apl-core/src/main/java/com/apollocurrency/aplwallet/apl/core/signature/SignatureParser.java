/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
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

    Signature parse(byte[] bytes);

    int calcDataSize(int count);

    byte[] bytes(Signature signature);

    /**
     * Return the json representation of the signature object
     *
     * @param signature
     * @return
     */
    static JSONObject getJsonObject(Signature signature) {
        JSONObject json = new JSONObject();
        json.put(SIGNATURE_FIELD_NAME, Convert.toHexString(signature.bytes()));
        return json;
    }

}
