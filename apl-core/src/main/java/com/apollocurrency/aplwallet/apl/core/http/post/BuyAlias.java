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
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasBuy;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;

@Vetoed
public final class BuyAlias extends CreateTransaction {
    private final AliasService aliasService;

    public BuyAlias() {
        super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName", "amountATM");
        this.aliasService = CDI.current().select(AliasService.class).get();
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Account buyer = HttpParameterParserUtil.getSenderAccount(req);
        Alias alias = HttpParameterParserUtil.getAlias(req);
        long amountATM = HttpParameterParserUtil.getAmountATM(req);
        if (aliasService.getOffer(alias) == null) {
            return INCORRECT_ALIAS_NOTFORSALE;
        }
        long sellerId = alias.getAccountId();
        Attachment attachment = new MessagingAliasBuy(alias.getAliasName());
        return createTransaction(req, buyer, sellerId, amountATM, attachment);
    }
}
