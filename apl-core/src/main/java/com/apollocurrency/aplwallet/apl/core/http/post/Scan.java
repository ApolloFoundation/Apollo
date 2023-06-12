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

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ScanEntity;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;

@Vetoed
public final class Scan extends AbstractAPIRequestHandler {

    public Scan() {
        super(new APITag[]{APITag.DEBUG}, "numBlocks", "height", "validate");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            boolean validate = "true".equalsIgnoreCase(req.getParameter("validate"));
            int numBlocks = 0;
            try {
                numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
            } catch (NumberFormatException ignored) {
            }
            int height = -1;
            try {
                height = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignore) {
            }
            long start = System.currentTimeMillis();
            lookupBlockchainProcessor();
            try {
                blockchainProcessor.suspendBlockchainDownloading();
                ScanEntity scanEntity = new ScanEntity(validate, 0, false);
                if (numBlocks > 0) {
                    scanEntity.setFromHeight(lookupBlockchain().getHeight() - numBlocks + 1);
                } else if (height >= 0) {
                    scanEntity.setFromHeight(height);
                } else {
                    return JSONResponses.missing("numBlocks", "height");
                }
                blockchainProcessor.scan(scanEntity);
            } finally {
                blockchainProcessor.resumeBlockchainDownloading();
            }
            long end = System.currentTimeMillis();
            response.put("done", true);
            response.put("scanTime", (end - start) / 1000);
        } catch (RuntimeException e) {
            JSONData.putException(response, e);
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
