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

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import javax.enterprise.inject.Vetoed;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Vetoed
public final class GetBlocks extends AbstractAPIRequestHandler {

    public GetBlocks() {
        super(new APITag[] {APITag.BLOCKS}, "firstIndex", "lastIndex", "timestamp", "includeTransactions", "includeExecutedPhased");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        int firstIndex = HttpParameterParser.getFirstIndex(req);
        int lastIndex = HttpParameterParser.getLastIndex(req);
        final int timestamp = HttpParameterParser.getTimestamp(req);
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeExecutedPhased = "true".equalsIgnoreCase(req.getParameter("includeExecutedPhased"));

        JSONArray blocks = new JSONArray();
        Block lastBlock = lookupBlockchain().getLastBlock();
        if (lastBlock != null) {
            try (DbIterator<? extends Block> iterator = lookupBlockchain().getBlocks(firstIndex, lastIndex)) {
                while (iterator.hasNext()) {
                    Block block = iterator.next();
                    if (block.getTimestamp() < timestamp) {
                        break;
                    }
                    blocks.add(JSONData.block(block, includeTransactions, includeExecutedPhased));
                }
            }
        } else {
            log.warn("Still no any blocks in db...");
        }
        JSONObject response = new JSONObject();
        response.put("blocks", blocks);

        return response;
    }

}
