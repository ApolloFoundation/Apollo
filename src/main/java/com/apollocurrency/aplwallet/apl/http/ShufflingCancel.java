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

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Shuffling;

public final class ShufflingCancel extends CreateTransaction {

    private static class ShufflingCancelHolder {
        private static final ShufflingCancel INSTANCE = new ShufflingCancel();
    }

    public static ShufflingCancel getInstance() {
        return ShufflingCancelHolder.INSTANCE;
    }

    private ShufflingCancel() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shuffling", "cancellingAccount", "shufflingStateHash");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req, boolean validate) throws AplException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        long cancellingAccountId = ParameterParser.getAccountId(req, "cancellingAccount", false);
        byte[] shufflingStateHash = ParameterParser.getBytes(req, "shufflingStateHash", validate);
        long accountId = ParameterParser.getAccountId(req, accountName2FA(), false);
        byte[] secretBytes = ParameterParser.getSecretBytes(req,accountId, validate);
//        TODO:perform fee calculation without using mock attachment
        Attachment.ShufflingCancellation attachment = validate ? shuffling.revealKeySeeds(secretBytes, cancellingAccountId, shufflingStateHash) :
                new Attachment.ShufflingCancellation(shuffling.getId(), new byte[0][0], new byte[0][0], shufflingStateHash, cancellingAccountId);
        Account account = ParameterParser.getSenderAccount(req, validate);
        return new CreateTransactionRequestData(attachment, account);
    }
}
