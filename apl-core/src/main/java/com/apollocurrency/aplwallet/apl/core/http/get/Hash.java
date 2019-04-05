/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
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

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class Hash extends AbstractAPIRequestHandler {

    public Hash() {
        super(new APITag[] {APITag.UTILS}, "hashAlgorithm", "secret", "secretIsText");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        byte algorithm = ParameterParser.getByte(req, "hashAlgorithm", (byte) 0, Byte.MAX_VALUE, false);
        HashFunction hashFunction = null;
        try {
            hashFunction = HashFunction.getHashFunction(algorithm);
        } catch (IllegalArgumentException ignore) {}
        if (hashFunction == null) {
            return JSONResponses.INCORRECT_HASH_ALGORITHM;
        }

        boolean secretIsText = "true".equalsIgnoreCase(req.getParameter("secretIsText"));
        byte[] secret;
        try {
            secret = secretIsText ? Convert.toBytes(req.getParameter("secret"))
                    : Convert.parseHexString(req.getParameter("secret"));
        } catch (RuntimeException e) {
            return JSONResponses.INCORRECT_SECRET;
        }
        if (secret == null || secret.length == 0) {
            return JSONResponses.MISSING_SECRET;
        }

        JSONObject response = new JSONObject();
        response.put("hash", Convert.toHexString(hashFunction.hash(secret)));
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
