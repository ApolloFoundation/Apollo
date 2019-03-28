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

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Search;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TAGGED_DATA_FILE;
import javax.enterprise.inject.Vetoed;
import static org.slf4j.LoggerFactory.getLogger;


@Vetoed
public final class DetectMimeType extends AbstractAPIRequestHandler {
    private static final Logger LOG = getLogger(DetectMimeType.class);

    public DetectMimeType() {
        super("file", new APITag[] {APITag.DATA, APITag.UTILS}, "data", "filename", "isText");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data"));
        byte[] data;
        if (dataValue == null) {
            try {
                Part part = req.getPart("file");
                if (part == null) {
                    throw new ParameterException(INCORRECT_TAGGED_DATA_FILE);
                }
                ParameterParser.FileData fileData = new ParameterParser.FileData(part).invoke();
                data = fileData.getData();
                // Depending on how the client submits the form, the filename, can be a regular parameter
                // or encoded in the multipart form. If its not a parameter we take from the form
                if (filename.isEmpty() && fileData.getFilename() != null) {
                    filename = fileData.getFilename();
                }
            } catch (IOException | ServletException e) {
                LOG.debug("error in reading file data", e);
                throw new ParameterException(INCORRECT_TAGGED_DATA_FILE);
            }
        } else {
            boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
            data = isText ? Convert.toBytes(dataValue) : Convert.parseHexString(dataValue);
        }

        JSONObject response = new JSONObject();
        response.put("type", Search.detectMimeType(data, filename));
        return response;
    }

    @Override
    protected boolean requirePost() {
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
