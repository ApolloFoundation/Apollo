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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.util.MemoryAppender;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import javax.enterprise.inject.Vetoed;

/**
 * <p>The GetLog API will return log messages from the ring buffer
 * maintained by the MemoryAppender log handler.  The most recent
 * 'count' messages will be returned.  All log messages in the
 * ring buffer will be returned if 'count' is omitted.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>count - The number of log messages to return</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>messages - An array of log messages</li>
 * </ul>
 */
@Vetoed
public final class GetLog extends AbstractAPIRequestHandler {

    /**
     * Create the GetLog instance
     */
    public GetLog() {
        super(new APITag[] {APITag.DEBUG}, "count");
    }

    /**
     * Process the GetLog API request
     *
     * @param   req                 API request
     * @return                      API response
     */
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        //
        // Get the number of log messages to return
        //
        int count;
        String value = req.getParameter("count");
        if (value != null)
            count = Math.max(Integer.valueOf(value), 0);
        else
            count = Integer.MAX_VALUE;
        //
        // Get the log messages
        //
        JSONArray logJSON = new JSONArray();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                Appender<ILoggingEvent> appender = index.next();
                if(appender.getName().equalsIgnoreCase("inMemory")){
                    MemoryAppender memoryAppender = (MemoryAppender) appender;
                    logJSON.addAll(memoryAppender.getMessages(count));
                }
            }
        }
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        response.put("messages", logJSON);
        return response;
    }

    /**
     * Require the administrator password
     *
     * @return                      TRUE if the admin password is required
     */
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
