/*
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
package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendUpdateTransaction extends CreateTransaction {

    private static class SendUpdateTransactionHolder {
        private static final SendUpdateTransaction INSTANCE = new SendUpdateTransaction();
    }

    public static SendUpdateTransaction getInstance() {
        return SendUpdateTransactionHolder.INSTANCE;
    }

    private SendUpdateTransaction() {
        super(new APITag[] {APITag.UPDATE, APITag.CREATE_TRANSACTION}, "architecture", "platform", "signature", "hash", "version", "urlFirstPart",
                "urlSecondPart",
                "level");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Architecture architecture = Architecture.valueOf(Convert.nullToEmpty(req.getParameter("architecture")).trim());
        Platform platform = Platform.valueOf(Convert.nullToEmpty(req.getParameter("platform")).trim());
        byte[] urlFirstPart = ParameterParser.getBytes(req, "urlFirstPart", true);
        byte[] urlSecondPart = ParameterParser.getBytes(req, "urlSecondPart", true);
        Version version = Version.from(Convert.nullToEmpty(req.getParameter("version")).trim());
        byte[] hash = ParameterParser.getBytes(req, "hash", true);
        byte level = ParameterParser.getByte(req, "level", (byte)0, Byte.MAX_VALUE, true);
        if (urlFirstPart.length != Constants.UPDATE_URL_PART_LENGTH) {
            return JSONResponses.INCORRECT_UPDATE_URL_FIRST_PART_LENGTH;
        }
        if (urlSecondPart.length != Constants.UPDATE_URL_PART_LENGTH) {
            return JSONResponses.INCORRECT_UPDATE_URL_SECOND_PART_LENGTH;
        }
        if (hash.length > Constants.MAX_UPDATE_HASH_LENGTH) {
            return JSONResponses.INCORRECT_UPDATE_HASH_LENGTH;
        }
        DoubleByteArrayTuple url = new DoubleByteArrayTuple(urlFirstPart, urlSecondPart);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = Attachment.UpdateAttachment.getAttachment(platform, architecture, url, version, hash, level);
        return createTransaction(req, account, attachment);
    }
}

