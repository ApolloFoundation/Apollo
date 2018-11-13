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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_PUBLIC_KEY;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Shuffling;
import com.apollocurrency.aplwallet.apl.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingProcess extends CreateTransaction {

    private static class ShufflingProcessHolder {
        private static final ShufflingProcess INSTANCE = new ShufflingProcess();
    }

    public static ShufflingProcess getInstance() {
        return ShufflingProcessHolder.INSTANCE;
    }

    private ShufflingProcess() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "shuffling", "recipientSecretPhrase", "recipientPublicKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 11);
            response.put("errorDescription", "Shuffling is not in processing, stage " + shuffling.getStage());
            return JSON.prepare(response);
        }
        Account senderAccount = ParameterParser.getSenderAccount(req);
        long senderId = senderAccount.getId();
        if (shuffling.getAssigneeAccountId() != senderId) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 12);
            response.put("errorDescription", String.format("Account %s cannot process shuffling since shuffling assignee is %s",
                    Convert.rsAccount(senderId), Convert.rsAccount(shuffling.getAssigneeAccountId())));
            return JSON.prepare(response);
        }
        ShufflingParticipant participant = shuffling.getParticipant(senderId);
        if (participant == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 13);
            response.put("errorDescription", String.format("Account %s is not a participant of shuffling %d",
                    Convert.rsAccount(senderId), shuffling.getId()));
            return JSON.prepare(response);
        }

        long accountId = ParameterParser.getAccountId(req, this.vaultAccountName(), false);
        byte[] secretBytes = ParameterParser.getSecretBytes(req,accountId, true);
        byte[] recipientPublicKey = ParameterParser.getPublicKey(req, "recipient");
        if (Account.getAccount(recipientPublicKey) != null) {
            return INCORRECT_PUBLIC_KEY; // do not allow existing account to be used as recipient
        }

        Attachment.ShufflingAttachment attachment = shuffling.process(senderId, secretBytes, recipientPublicKey);
        return createTransaction(req, senderAccount, attachment);
    }

}
