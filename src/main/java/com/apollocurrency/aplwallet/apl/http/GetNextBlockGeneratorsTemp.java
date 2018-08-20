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
import com.apollocurrency.aplwallet.apl.Blockchain;
import com.apollocurrency.aplwallet.apl.Generator;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * The GetNextBlockGenerators API will return the next block generators ordered by the
 * hit time.  The list of active forgers is initialized using the block generators
 * with at least 2 blocks generated within the previous 10,000 blocks.  Accounts without
 * a public key will not be included.  The list is
 * updated as new blocks are processed.  This means the results will not be 100%
 * correct since previously active generators may no longer be running and new generators
 * won't be known until they generate a block.  This API will be replaced when transparent
 * forging is activated.
 * <p>
 * Request parameters:
 * <ul>
 * <li>limit - The number of forgers to return and defaults to 1.
 * </ul>
 * <p>
 * Return fields:
 * <ul>
 * <li>activeCount - The number of active generators
 * <li>height - The last block height
 * <li>lastBlock - The last block identifier
 * <li>timestamp - The last block timestamp
 * <li>generators - The next block generators
 * <ul>
 * <li>account - The account identifier
 * <li>accountRS - The account RS identifier
 * <li>deadline - The difference between the generation time and the last block timestamp
 * <li>effectiveBalanceAPL - The account effective balance
 * <li>hitTime - The generation time for the account
 * </ul>
 * </ul>
 */
public final class GetNextBlockGeneratorsTemp extends APIServlet.APIRequestHandler {

    private static class GetNextBlockGeneratorsTempHolder {
        private static final GetNextBlockGeneratorsTemp INSTANCE = new GetNextBlockGeneratorsTemp();
    }

    public static GetNextBlockGeneratorsTemp getInstance() {
        return GetNextBlockGeneratorsTempHolder.INSTANCE;
    }

    private GetNextBlockGeneratorsTemp() {
        super(new APITag[] {APITag.FORGING}, "limit");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        JSONObject response = new JSONObject();
        int limit = Math.max(1, ParameterParser.getInt(req, "limit", 1, Integer.MAX_VALUE, false));
        Blockchain blockchain = Apl.getBlockchain();
        blockchain.readLock();
        try {
            Block lastBlock = blockchain.getLastBlock();
            response.put("timestamp", lastBlock.getTimestamp());
            response.put("height", lastBlock.getHeight());
            response.put("lastBlock", Long.toUnsignedString(lastBlock.getId()));
            List<Generator.ActiveGenerator> activeGenerators = Generator.getNextGenerators();
            response.put("activeCount", activeGenerators.size());
            JSONArray generators = new JSONArray();
            for (Generator.ActiveGenerator generator : activeGenerators) {
                if (generator.getHitTime() > Integer.MAX_VALUE) {
                    break;
                }
                JSONObject resp = new JSONObject();
                JSONData.putAccount(resp, "account", generator.getAccountId());
                resp.put("effectiveBalanceAPL", generator.getEffectiveBalance());
                resp.put("hitTime", generator.getHitTime());
                resp.put("deadline", (int)generator.getHitTime() - lastBlock.getTimestamp());
                generators.add(resp);
                if (generators.size() == limit) {
                    break;
                }
            }
            response.put("generators", generators);
        } finally {
            blockchain.readUnlock();
        }
        return response;
    }

    /**
     * No required block parameters
     *
     * @return                      FALSE to disable the required block parameters
     */
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
