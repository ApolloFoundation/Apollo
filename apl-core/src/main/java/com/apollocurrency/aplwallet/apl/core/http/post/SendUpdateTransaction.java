/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Platform;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONStreamAware;

@Vetoed
public final class SendUpdateTransaction extends CreateTransaction {

    public SendUpdateTransaction() {
        super(new APITag[] {APITag.UPDATE, APITag.CREATE_TRANSACTION}, "architecture", "platform", "hash", "version", "urlFirstPart",
                "urlSecondPart",
                "level");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Architecture architecture = Architecture.valueOf(Convert.nullToEmpty(req.getParameter("architecture")).trim());
        Platform platform = Platform.valueOf(Convert.nullToEmpty(req.getParameter("platform")).trim());
        byte[] urlFirstPart = ParameterParser.getBytes(req, "urlFirstPart", true);
        byte[] urlSecondPart = ParameterParser.getBytes(req, "urlSecondPart", true);
        Version version = new Version(Convert.nullToEmpty(req.getParameter("version")).trim());
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
        Attachment attachment = UpdateAttachment.getAttachment(platform, architecture, url, version, hash, level);
        return createTransaction(req, account, attachment);
    }
}

