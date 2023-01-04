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
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;

@Vetoed
public final class GetSharedKey extends AbstractAPIRequestHandler {

    public GetSharedKey() {
        super(new APITag[]{APITag.MESSAGES}, "account", "secretPhrase", "nonce", "passphrase", "participantAccount");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, false);

        long participantAccountId = HttpParameterParserUtil.getAccountId(req, "participantAccount", true);
        byte[] nonce = HttpParameterParserUtil.getBytes(req, "nonce", true);
        byte[] publicKey = lookupAccountService().getPublicKeyByteArray(participantAccountId);
        if (publicKey == null) {
            return JSONResponses.INCORRECT_ACCOUNT;
        }
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(keySeed), publicKey, nonce);
        JSONObject response = new JSONObject();
        response.put("sharedKey", Convert.toHexString(sharedKey));
        return response;

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
