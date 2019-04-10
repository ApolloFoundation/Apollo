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

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import javax.enterprise.inject.Vetoed;

@Vetoed
public final class PopOff extends AbstractAPIRequestHandler {

    public PopOff() {
        super(new APITag[] {APITag.DEBUG}, "numBlocks", "height", "keepTransactions");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        int numBlocks = 0;
        try {
            numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
        } catch (NumberFormatException ignored) {}
        int height = 0;
        try {
            height = Integer.parseInt(req.getParameter("height"));
        } catch (NumberFormatException ignored) {}
        boolean keepTransactions = "true".equalsIgnoreCase(req.getParameter("keepTransactions"));
        List<? extends Block> blocks;
        BlockchainProcessor blockchainProcessor = lookupBlockchainProcessor();
        try {
            blockchainProcessor.setGetMoreBlocks(false);
            if (numBlocks > 0) {
                blocks = blockchainProcessor.popOffTo(lookupBlockchain().getHeight() - numBlocks);
            } else if (height > 0) {
                blocks = blockchainProcessor.popOffTo(height);
            } else {
                return JSONResponses.missing("numBlocks", "height");
            }
        } finally {
            blockchainProcessor.setGetMoreBlocks(true);
        }
        JSONArray blocksJSON = new JSONArray();
        blocks.forEach(block -> blocksJSON.add(JSONData.block(block, true, false)));
        JSONObject response = new JSONObject();
        response.put("blocks", blocksJSON);
        if (keepTransactions) {
            blocks.forEach(block -> lookupTransactionProcessor().processLater(block.getTransactions()));
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
