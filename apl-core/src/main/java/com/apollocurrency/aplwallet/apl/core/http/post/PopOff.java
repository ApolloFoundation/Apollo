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
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import javax.enterprise.inject.Vetoed;

@Slf4j
@Vetoed
public final class PopOff extends AbstractAPIRequestHandler {

    public PopOff() {
        super(new APITag[]{APITag.DEBUG}, "numBlocks", "height", "keepTransactions");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        int numBlocks = 0;
        try {
            numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
        } catch (NumberFormatException ignored) {
        }
        int height = 0;
        try {
            height = Integer.parseInt(req.getParameter("height"));
        } catch (NumberFormatException ignored) {
        }
        boolean keepTransactions = "true".equalsIgnoreCase(req.getParameter("keepTransactions"));
        List<? extends Block> blocks;
        lookupBlockchainProcessor();
        try {
            blockchainProcessor.suspendBlockchainDownloading();
            //TODO: It's a temporary approach to prevent hanging on calling the waitTrimming method.
            // It needs to look for the thread that keeps the readLock so long time.
            _waitForSuitableConditionBeforePopOff();

            if (numBlocks > 0) {
                height = lookupBlockchain().getHeight() - numBlocks;
                log.trace(">> PopOff by 'numBlocks' to height = {}", height);
                blocks = blockchainProcessor.popOffTo(height);
            } else if (height > 0) {
                log.trace(">> PopOff to exact 'height' = {}", height);
                blocks = blockchainProcessor.popOffTo(height);
            } else {
                return JSONResponses.missing("numBlocks", "height");
            }
            log.trace("<< PopOff to height = {}", height);
        } finally {
            blockchainProcessor.resumeBlockchainDownloading();
        }
        //usually we do not need those blocks in output
        //JSONArray blocksJSON = new JSONArray();
        //blocks.forEach(block -> blocksJSON.add(JSONData.block(block, true, false)));
        JSONObject response = new JSONObject();
        //response.put("blocks", blocksJSON);
        if (keepTransactions) {
            blocks.forEach(block -> lookupTransactionProcessor().processLater(block.getOrLoadTransactions()));
        }
        return response;
    }

    private void _waitForSuitableConditionBeforePopOff() {
        blockchainProcessor.waitUntilBlockchainDownloadingStops(); //No events 'onBlockPushed' are generated here
        lookupTrimService();
        trimService.updateTrimConfig(true, true);//clear trim heights queue
        trimService.waitTrimming();//to prevent eventual deadlock
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
