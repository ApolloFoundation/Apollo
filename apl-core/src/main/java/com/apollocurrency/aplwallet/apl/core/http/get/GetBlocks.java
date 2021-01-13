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

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Deprecated
@Slf4j
@Vetoed
public final class GetBlocks extends AbstractAPIRequestHandler {

    public GetBlocks() {
        super(new APITag[]{APITag.BLOCKS}, "firstIndex", "lastIndex", "timestamp", "includeTransactions", "includeExecutedPhased");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        final int timestamp = HttpParameterParserUtil.getTimestamp(req);
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeExecutedPhased = "true".equalsIgnoreCase(req.getParameter("includeExecutedPhased"));

        JSONArray blocks = new JSONArray();
        Block lastBlock = lookupBlockchain().getLastBlock();
        if (lastBlock != null) {
            List<Block> blockchainBlocks = lookupBlockchain().getBlocks(firstIndex, lastIndex, timestamp);
            for (Block blockchainBlock : blockchainBlocks) {
                if (blockchainBlock.getTimestamp() < timestamp) { // not needed after 'timestamp' is added as SQL param above
                    break;
                }
                blocks.add(JSONData.block(blockchainBlock, includeTransactions, includeExecutedPhased));
            }
        } else {
            log.warn("Still no any blocks in db...");
        }
        JSONObject response = new JSONObject();
        response.put("blocks", blocks);

        return response;
    }

}
