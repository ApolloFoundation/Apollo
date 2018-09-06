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

import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_BLOCK;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_HEIGHT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_BLOCK;

public final class GetBlock extends APIServlet.APIRequestHandler {

    private static class GetBlockHolder {
        private static final GetBlock INSTANCE = new GetBlock();
    }

    public static GetBlock getInstance() {
        return GetBlockHolder.INSTANCE;
    }

    private GetBlock() {
        super(new APITag[] {APITag.BLOCKS}, "block", "height", "timestamp", "includeTransactions", "includeExecutedPhased");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        Block blockData;
        String blockValue = Convert.emptyToNull(req.getParameter("block"));
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        String timestampValue = Convert.emptyToNull(req.getParameter("timestamp"));
        if (blockValue != null) {
            try {
                blockData = Apl.getBlockchain().getBlock(Convert.parseUnsignedLong(blockValue));
            } catch (RuntimeException e) {
                return INCORRECT_BLOCK;
            }
        } else if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > Apl.getBlockchain().getHeight()) {
                    return INCORRECT_HEIGHT;
                }
                blockData = Apl.getBlockchain().getBlockAtHeight(height);
            } catch (RuntimeException e) {
                return INCORRECT_HEIGHT;
            }
        } else if (timestampValue != null) {
            try {
                int timestamp = Integer.parseInt(timestampValue);
                if (timestamp < 0) {
                    return INCORRECT_TIMESTAMP;
                }
                blockData = Apl.getBlockchain().getLastBlock(timestamp);
            } catch (RuntimeException e) {
                return INCORRECT_TIMESTAMP;
            }
        } else {
            blockData = Apl.getBlockchain().getLastBlock();
        }

        if (blockData == null) {
            return UNKNOWN_BLOCK;
        }

        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeExecutedPhased = "true".equalsIgnoreCase(req.getParameter("includeExecutedPhased"));

        return JSONData.block(blockData, includeTransactions, includeExecutedPhased);

    }

}
