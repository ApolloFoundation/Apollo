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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Shuffling;
import com.apollocurrency.aplwallet.apl.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

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
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 11);
            response.put("errorDescription", "Shuffling is not in processing, stage " + shuffling.getStage());
            return new CreateTransactionRequestData(JSON.prepare(response));
        }
        Account senderAccount = ParameterParser.getSenderAccount(req, validate);
        long senderId = senderAccount.getId();
        if (validate && shuffling.getAssigneeAccountId() != senderId) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 12);
            response.put("errorDescription", String.format("Account %s cannot process shuffling since shuffling assignee is %s",
                    Convert.rsAccount(senderId), Convert.rsAccount(shuffling.getAssigneeAccountId())));
            return new CreateTransactionRequestData(JSON.prepare(response));
        }
        ShufflingParticipant participant = shuffling.getParticipant(senderId);
        if (participant == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 13);
            response.put("errorDescription", String.format("Account %s is not a participant of shuffling %d",
                    Convert.rsAccount(senderId), shuffling.getId()));
            return new CreateTransactionRequestData(JSON.prepare(response));
        }

        long accountId = ParameterParser.getAccountId(req, accountName2FA(), false);
        long recipientId = ParameterParser.getAccountId(req, "recipient", false);
        byte[] secretBytes = ParameterParser.getSecretBytes(req,accountId, validate);
        byte[] recipientPublicKey = ParameterParser.getPublicKey(req, "recipient", recipientId, validate);
        if (validate && Account.getAccount(recipientPublicKey) != null) {
            return new CreateTransactionRequestData(INCORRECT_PUBLIC_KEY); // do not allow existing account to be used as recipient
        }

//        TODO: perform fee calculation without mock attachment
        Attachment.ShufflingAttachment attachment = validate ? shuffling.process(senderId, secretBytes, recipientPublicKey) :
                new Attachment.ShufflingProcessing(shuffling.getId(), new byte[0][0], shuffling.getStateHash());
        return new CreateTransactionRequestData(attachment, senderAccount);
    }

}
