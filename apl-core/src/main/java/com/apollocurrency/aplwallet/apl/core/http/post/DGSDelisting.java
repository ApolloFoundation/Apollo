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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_GOODS;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.dgs.DGSService;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelisting;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class DGSDelisting extends CreateTransaction {

    public DGSDelisting() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION}, "goods");
    }
    private DGSService service = CDI.current().select(DGSService.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Account account = ParameterParser.getSenderAccount(req);
        DGSGoods goods = ParameterParser.getGoods(service, req);
        if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
            return UNKNOWN_GOODS;
        }
        Attachment attachment = new DigitalGoodsDelisting(goods.getId());
        return createTransaction(req, account, attachment);
    }

}
