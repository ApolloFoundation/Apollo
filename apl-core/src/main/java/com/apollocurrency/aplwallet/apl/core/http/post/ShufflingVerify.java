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
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingVerificationAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Vetoed
public final class ShufflingVerify extends CreateTransactionHandler {

    public ShufflingVerify() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shuffling", "shufflingStateHash");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Shuffling shuffling = HttpParameterParserUtil.getShuffling(req);
        byte[] shufflingStateHash = HttpParameterParserUtil.getBytes(req, "shufflingStateHash", true);
        if (!Arrays.equals(shufflingStateHash, shufflingService.getStageHash(shuffling))) {
            return JSONResponses.incorrect("shufflingStateHash", "Shuffling is in a different state now");
        }
        Attachment attachment = new ShufflingVerificationAttachment(shuffling.getId(), shufflingStateHash);

        Account account = HttpParameterParserUtil.getSenderAccount(req);
        return createTransaction(req, account, attachment);
    }
}
