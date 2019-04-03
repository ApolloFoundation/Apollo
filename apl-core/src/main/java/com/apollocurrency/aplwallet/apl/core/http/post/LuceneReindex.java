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

import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.sql.Connection;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

public final class LuceneReindex extends AbstractAPIRequestHandler {
    private final FullTextSearchService fullTextSearchProvider = CDI.current().select(FullTextSearchService.class).get();
    private static class LuceneReindexHolder {
        private static final LuceneReindex INSTANCE = new LuceneReindex();
    }

    public static LuceneReindex getInstance() {
        return LuceneReindexHolder.INSTANCE;
    }

    private LuceneReindex() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try (Connection con = lookupDataSource().getConnection()) {
            fullTextSearchProvider.reindexAll(con);
            response.put("done", true);
        } catch (SQLException e) {
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
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
