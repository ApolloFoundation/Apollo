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

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AccountControlEffectiveBalanceLeasing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

@Deprecated
@Vetoed
public final class LeaseBalance extends CreateTransactionHandler {

    public LeaseBalance() {
        super(new APITag[]{APITag.FORGING, APITag.ACCOUNT_CONTROL, APITag.CREATE_TRANSACTION}, "period", "recipient");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        int period = HttpParameterParserUtil.getInt(req, "period", blockchainConfig.getLeasingDelay(), 65535, true);
        Account account = HttpParameterParserUtil.getSenderAccount(req);
        long recipient = HttpParameterParserUtil.getAccountId(req, "recipient", true);
        Account recipientAccount = lookupAccountService().getAccount(recipient);
        if (recipientAccount == null || lookupAccountService().getPublicKeyByteArray(recipientAccount.getId()) == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "recipient account does not have public key");
            return response;
        }
        Attachment attachment = new AccountControlEffectiveBalanceLeasing(period);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
