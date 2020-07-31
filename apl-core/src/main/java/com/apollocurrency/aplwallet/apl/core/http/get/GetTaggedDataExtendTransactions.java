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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Vetoed
public final class GetTaggedDataExtendTransactions extends AbstractAPIRequestHandler {

    private TaggedDataService taggedDataService = CDI.current().select(TaggedDataService.class).get();

    public GetTaggedDataExtendTransactions() {
        super(new APITag[]{APITag.DATA}, "transaction");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long taggedDataId = HttpParameterParserUtil.getUnsignedLong(req, "transaction", true);
        List<TaggedDataExtend> extendTransactionList = taggedDataService.getExtendTransactionIds(taggedDataId);
        List<Long> extendTransactions = extendTransactionList.stream().map(TaggedDataExtend::getTaggedDataId).collect(Collectors.toList());
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Blockchain blockchain = lookupBlockchain();
        Filter<Appendix> filter = (appendix) -> !(appendix instanceof TaggedDataExtendAttachment);
        extendTransactions.forEach(transactionId -> jsonArray.add(JSONData.transaction(blockchain.getTransaction(transactionId), filter, false)));
        response.put("extendTransactions", jsonArray);
        return response;
    }

}
