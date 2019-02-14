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

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistration;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingRegister extends CreateTransaction {

    private static class ShufflingRegisterHolder {
        private static final ShufflingRegister INSTANCE = new ShufflingRegister();
    }

    public static ShufflingRegister getInstance() {
        return ShufflingRegisterHolder.INSTANCE;
    }

    private ShufflingRegister() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shufflingFullHash");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", true);

        Attachment attachment = new ShufflingRegistration(shufflingFullHash);

        Account account = ParameterParser.getSenderAccount(req);
        if (account.getControls().contains(Account.ControlType.PHASING_ONLY)) {
            return JSONResponses.error("Accounts under phasing only control cannot join a shuffling");
        }
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            Shuffling shuffling = Shuffling.getShuffling(shufflingFullHash);
            if (shuffling == null) {
                return JSONResponses.NOT_ENOUGH_FUNDS;
            }
            return JSONResponses.notEnoughHolding(shuffling.getHoldingType());
        }
    }

}
