/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */
package apl.http;

import apl.*;
import apl.updater.Architecture;
import apl.updater.Platform;
import apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendUpdateTransaction extends CreateTransaction {

    static final SendUpdateTransaction instance = new SendUpdateTransaction();

    private SendUpdateTransaction() {
        super(new APITag[] {APITag.UPDATE, APITag.CREATE_TRANSACTION}, "architecture", "platform", "signature", "hash", "version", "url", "level");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Architecture architecture = Architecture.valueOf(Convert.nullToEmpty(req.getParameter("architecture")).trim());
        Platform platform = Platform.valueOf(Convert.nullToEmpty(req.getParameter("platform")).trim());
        String url = Convert.nullToEmpty(req.getParameter("url")).trim();
        Version version = Version.from(Convert.nullToEmpty(req.getParameter("version")).trim());
        byte[] signature = ParameterParser.getBytes(req, "signature", true);
        int hash = ParameterParser.getInt(req, "hash", Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        byte level = ParameterParser.getByte(req, "level", (byte)0, Byte.MAX_VALUE, true);
        if (url.isEmpty() || url.length() > Constants.MAX_UPDATE_URL_LENGTH) {
            return JSONResponses.INCORRECT_UPDATE_URL_LENGTH;
        }
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = Attachment.UpdateAttachment.getAttachment(platform, architecture, url, version, hash, signature, level);
        return createTransaction(req, account, attachment);
    }
}

