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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_PUBLIC_KEY;

@Vetoed
public final class ShufflingProcess extends CreateTransaction {

    public ShufflingProcess() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
            "shuffling", "recipientSecretPhrase", "recipientPublicKey");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Shuffling shuffling = HttpParameterParserUtil.getShuffling(req);
        if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 11);
            response.put("errorDescription", "Shuffling is not in processing, stage " + shuffling.getStage());
            return JSON.prepare(response);
        }
        Account senderAccount = HttpParameterParserUtil.getSenderAccount(req);
        long senderId = senderAccount.getId();
        if (shuffling.getAssigneeAccountId() != senderId) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 12);
            response.put("errorDescription", String.format("Account %s cannot process shuffling since shuffling assignee is %s",
                Convert2.rsAccount(senderId), Convert2.rsAccount(shuffling.getAssigneeAccountId())));
            return JSON.prepare(response);
        }
        ShufflingParticipant participant = shufflingService.getParticipant(shuffling.getId(), senderId);
        if (participant == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 13);
            response.put("errorDescription", String.format("Account %s is not a participant of shuffling %d",
                Convert2.rsAccount(senderId), shuffling.getId()));
            return JSON.prepare(response);
        }

        long accountId = HttpParameterParserUtil.getAccountId(req, this.vaultAccountName(), false);
        byte[] secretBytes = HttpParameterParserUtil.getSecretBytes(req, accountId, true);
        byte[] recipientPublicKey = HttpParameterParserUtil.getPublicKey(req, "recipient");
        if (lookupAccountService().getAccount(recipientPublicKey) != null) {
            return INCORRECT_PUBLIC_KEY; // do not allow existing account to be used as recipient
        }

        ShufflingAttachment attachment = shufflingService.processShuffling(shuffling, senderId, secretBytes, recipientPublicKey);
        return createTransaction(req, senderAccount, attachment);
    }

}
