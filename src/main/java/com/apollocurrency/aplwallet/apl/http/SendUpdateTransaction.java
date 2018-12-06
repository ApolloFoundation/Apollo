/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;

public final class SendUpdateTransaction extends CreateTransaction {

    private static class SendUpdateTransactionHolder {
        private static final SendUpdateTransaction INSTANCE = new SendUpdateTransaction();
    }

    public static SendUpdateTransaction getInstance() {
        return SendUpdateTransactionHolder.INSTANCE;
    }

    private SendUpdateTransaction() {
        super(new APITag[] {APITag.UPDATE, APITag.CREATE_TRANSACTION}, "architecture", "platform", "hash", "version", "urlFirstPart",
                "urlSecondPart",
                "level");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {
        Architecture architecture = Architecture.valueOf(Convert.nullToEmpty(req.getParameter("architecture")).trim());
        Platform platform = Platform.valueOf(Convert.nullToEmpty(req.getParameter("platform")).trim());
        byte[] urlFirstPart = ParameterParser.getBytes(req, "urlFirstPart", true);
        byte[] urlSecondPart = ParameterParser.getBytes(req, "urlSecondPart", true);
        Version version = Version.from(Convert.nullToEmpty(req.getParameter("version")).trim());
        byte[] hash = ParameterParser.getBytes(req, "hash", true);
        byte level = ParameterParser.getByte(req, "level", (byte)0, Byte.MAX_VALUE, true);
        if (urlFirstPart.length != Constants.UPDATE_URL_PART_LENGTH) {
            return new CreateTransactionRequestData(JSONResponses.INCORRECT_UPDATE_URL_FIRST_PART_LENGTH);
        }
        if (urlSecondPart.length != Constants.UPDATE_URL_PART_LENGTH) {
            return new CreateTransactionRequestData(JSONResponses.INCORRECT_UPDATE_URL_SECOND_PART_LENGTH);
        }
        if (hash.length > Constants.MAX_UPDATE_HASH_LENGTH) {
            return new CreateTransactionRequestData(JSONResponses.INCORRECT_UPDATE_HASH_LENGTH);
        }
        DoubleByteArrayTuple url = new DoubleByteArrayTuple(urlFirstPart, urlSecondPart);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = Attachment.UpdateAttachment.getAttachment(platform, architecture, url, version, hash, level);
        return new CreateTransactionRequestData(attachment, account);
    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {
        byte level = ParameterParser.getByte(req, "level", (byte)0, Byte.MAX_VALUE, false);
        return new CreateTransactionRequestData(Attachment.UpdateAttachment.getAttachment(null, null, null, null, null, level), null);
    }
}

