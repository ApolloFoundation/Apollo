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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.DECRYPTION_FAILED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ACCOUNT;
import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

public final class DecryptFrom extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(DecryptFrom.class);

    private static class DecryptFromHolder {
        private static final DecryptFrom INSTANCE = new DecryptFrom();
    }

    public static DecryptFrom getInstance() {
        return DecryptFromHolder.INSTANCE;
    }

    private DecryptFrom() {
        super(new APITag[] {APITag.MESSAGES}, "participantAccount", "data", "nonce", "decryptedMessageIsText", "uncompressDecryptedMessage",
                "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        byte[] publicKey = Account.getPublicKey(ParameterParser.getAccountId(req, "participantAccount",true));
        if (publicKey == null) {
            return INCORRECT_ACCOUNT;
        }
        long accountId = ParameterParser.getAccountId(req, false);
        byte[] keySeed = ParameterParser.getKeySeed(req, accountId, true);
        byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("data")));
        byte[] nonce = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("nonce")));
        EncryptedData encryptedData = new EncryptedData(data, nonce);
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("decryptedMessageIsText"));
        boolean uncompress = !"false".equalsIgnoreCase(req.getParameter("uncompressDecryptedMessage"));
        try {
            byte[] decrypted = Account.decryptFrom(publicKey, encryptedData, keySeed, uncompress);
            JSONObject response = new JSONObject();
            response.put("decryptedMessage", isText ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
            return response;
        } catch (RuntimeException e) {
            LOG.debug(e.toString());
            return DECRYPTION_FAILED;
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
