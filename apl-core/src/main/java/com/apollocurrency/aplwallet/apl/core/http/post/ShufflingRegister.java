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
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class ShufflingRegister extends CreateTransaction {

    public ShufflingRegister() {
        super(new APITag[]{APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shufflingFullHash");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] shufflingFullHash = HttpParameterParserUtil.getBytes(req, "shufflingFullHash", true);

        Attachment attachment = new ShufflingRegistration(shufflingFullHash);

        Account account = HttpParameterParserUtil.getSenderAccount(req);
        if (account.getControls().contains(AccountControlType.PHASING_ONLY)) {
            return JSONResponses.error("Accounts under phasing only control cannot join a shuffling");
        }
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            Shuffling shuffling = shufflingService.getShuffling(shufflingFullHash);
            if (shuffling == null) {
                return JSONResponses.NOT_ENOUGH_APL;
            }
            return JSONResponses.notEnoughHolding(shuffling.getHoldingType());
        }
    }

}
