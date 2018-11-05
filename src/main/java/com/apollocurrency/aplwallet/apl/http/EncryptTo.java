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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_MESSAGE_TO_ENCRYPT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_RECIPIENT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_MESSAGE_TO_ENCRYPT;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

public final class EncryptTo extends APIServlet.APIRequestHandler {

    private static class EncryptToHolder {
        private static final EncryptTo INSTANCE = new EncryptTo();
    }

    public static EncryptTo getInstance() {
        return EncryptToHolder.INSTANCE;
    }

    private EncryptTo() {
        super(new APITag[] {APITag.MESSAGES}, "recipient", "messageToEncrypt", "messageToEncryptIsText", "compressMessageToEncrypt", "secretPhrase"
                , "account", "passphrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long senderAccountId = ParameterParser.getAccountId(req, "account", false);
        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        byte[] recipientPublicKey = Account.getPublicKey(recipientId);
        if (recipientPublicKey == null) {
            return INCORRECT_RECIPIENT;
        }
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        boolean compress = !"false".equalsIgnoreCase(req.getParameter("compressMessageToEncrypt"));
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncrypt"));
        if (plainMessage == null) {
            return MISSING_MESSAGE_TO_ENCRYPT;
        }
        byte[] plainMessageBytes;
        try {
            plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
        } catch (RuntimeException e) {
            return INCORRECT_MESSAGE_TO_ENCRYPT;
        }
        byte[] keySeed = ParameterParser.getKeySeed(req, senderAccountId, true);
        EncryptedData encryptedData = Account.encryptTo(recipientPublicKey, plainMessageBytes, keySeed, compress);
        return JSONData.encryptedData(encryptedData);

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
