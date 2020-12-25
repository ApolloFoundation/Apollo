/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface SignatureParser {
    String SIGNATURE_FIELD_NAME = "signature";

    default SignatureParser initParams(List<String> params){
        return this;
    }

    /**
     * Parse the byte array and build the signature object
     * @deprecated
     * @param buffer input data array
     * @return the signature object
     */
    @Deprecated(since = "TransactionV3")
    Signature parse(ByteBuffer buffer);

    Signature parse(byte[] bytes);

    default Signature parse(RlpReader reader) {
        throw new UnsupportedOperationException("Unsupported RLP format for signature.");
    }

    /**
     * Returns V1 or V2 signature size.
     * Don't use for RLP encoded signature (V3).
     * @param count the participant count
     * @return size of V1 or V2 signature
     */
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
