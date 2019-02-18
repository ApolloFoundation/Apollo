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

import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.EITHER_MESSAGE_ENCRYPTED_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_MESSAGE_ENCRYPTED_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class VerifyPrunableMessage extends AbstractAPIRequestHandler {

    private static class VerifyPrunableMessageHolder {
        private static final VerifyPrunableMessage INSTANCE = new VerifyPrunableMessage();
    }

    public static VerifyPrunableMessage getInstance() {
        return VerifyPrunableMessageHolder.INSTANCE;
    }

    private static final JSONStreamAware NO_SUCH_PLAIN_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no plain message attachment");
        NO_SUCH_PLAIN_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware NO_SUCH_ENCRYPTED_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no encrypted message attachment");
        NO_SUCH_ENCRYPTED_MESSAGE = JSON.prepare(response);
    }

    private VerifyPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction",
                "message", "messageIsText",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "compressMessageToEncrypt", "account");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = lookupBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }
        long account = ParameterParser.getAccountId(req, "account", false);
        PrunablePlainMessageAppendix plainMessage = (PrunablePlainMessageAppendix) ParameterParser.getPlainMessage(req, true);
        PrunableEncryptedMessageAppendix encryptedMessage = (PrunableEncryptedMessageAppendix) ParameterParser.getEncryptedMessage(req, null,
                account,true);

        if (plainMessage == null && encryptedMessage == null) {
            return MISSING_MESSAGE_ENCRYPTED_MESSAGE;
        }
        if (plainMessage != null && encryptedMessage != null) {
            return EITHER_MESSAGE_ENCRYPTED_MESSAGE;
        }

        if (plainMessage != null) {
            PrunablePlainMessageAppendix myPlainMessage = transaction.getPrunablePlainMessage();
            if (myPlainMessage == null) {
                return NO_SUCH_PLAIN_MESSAGE;
            }
            if (!Arrays.equals(myPlainMessage.getHash(), plainMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myPlainMessage.getJSONObject();
            response.put("verify", true);
            return response;
        } else if (encryptedMessage != null) {
            PrunableEncryptedMessageAppendix myEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (myEncryptedMessage == null) {
                return NO_SUCH_ENCRYPTED_MESSAGE;
            }
            if (!Arrays.equals(myEncryptedMessage.getHash(), encryptedMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myEncryptedMessage.getJSONObject();
            response.put("verify", true);
            return response;
        }

        return JSON.emptyJSON;
    }

}
