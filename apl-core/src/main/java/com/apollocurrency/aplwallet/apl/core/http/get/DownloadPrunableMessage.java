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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRUNED_TRANSACTION;
import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.PrunableMessage;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

@Vetoed
public final class DownloadPrunableMessage extends AbstractAPIRequestHandler {
        private static final Logger LOG = getLogger(DownloadPrunableMessage.class);

    public DownloadPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase", "sharedKey", "retrieve", "save", "account", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws AplException {
        long transactionId = ParameterParser.getUnsignedLong(request, "transaction", true);
        boolean retrieve = "true".equalsIgnoreCase(request.getParameter("retrieve"));
        PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
        if (prunableMessage == null && retrieve) {
            if (lookupBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                return PRUNED_TRANSACTION;
            }
            prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
        }
        long accountId = ParameterParser.getAccountId(request, false);
        byte[] keySeed = ParameterParser.getKeySeed(request, accountId, false);
        byte[] sharedKey = ParameterParser.getBytes(request, "sharedKey", false);
        if (sharedKey.length != 0 && keySeed != null) {
            return JSONResponses.either("secretPhrase", "sharedKey", "passphrase & account");
        }
        byte[] data = null;
        if (prunableMessage != null) {
            try {
                if (keySeed != null) {
                    data = prunableMessage.decryptUsingKeySeed(keySeed);
                } else if (sharedKey.length > 0) {
                    data = prunableMessage.decryptUsingSharedKey(sharedKey);
                } else {
                    data = prunableMessage.getMessage();
                }
            } catch (RuntimeException e) {
                LOG.debug("Decryption of message to recipient failed: " + e.toString());
                return JSONResponses.error("Wrong secretPhrase or sharedKey or passphrase");
            }
        }
        if (data == null) {
            data = Convert.EMPTY_BYTE;
        }
        String contentDisposition = "true".equalsIgnoreCase(request.getParameter("save")) ? "attachment" : "inline";
        response.setHeader("Content-Disposition", contentDisposition + "; filename=" + Long.toUnsignedString(transactionId));
        response.setContentLength(data.length);
        try (OutputStream out = response.getOutputStream()) {
            try {
                out.write(data);
            } catch (IOException e) {
                throw new ParameterException(JSONResponses.RESPONSE_WRITE_ERROR);
            }
        } catch (IOException e) {
            throw new ParameterException(JSONResponses.RESPONSE_STREAM_ERROR);
        }
        return null;
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        throw new UnsupportedOperationException();
    }
}
