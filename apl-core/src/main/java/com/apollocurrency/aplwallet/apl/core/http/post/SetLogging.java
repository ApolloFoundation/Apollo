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

import javax.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.JSON;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.LoggerFactory;

/**
 * <p>The SetLogging API will set the ARS log level for all log messages.
 * It will also set the communication events that are logged.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>logLevel - Specifies the log message level and defaults to INFO if not specified.</li>
 * <li>communicationEvent - Specifies a communication event to be logged and defaults to
 * no communication logging if not specified.
 * This parameter can be specified multiple times to log multiple communication events.</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>loggingUpdated - Set to 'true' if the logging was updated.</li>
 * </ul>
 *
 * <p>The following log levels can be specified:</p>
 * <ul>
 * <li>DEBUG - Debug, informational, warning and error messages will be logged.</li>
 * <li>INFO  - Informational, warning and error messages will be logged.</li>
 * <li>WARN  - Warning and error messages will be logged.</li>
 * <li>ERROR - Error messages will be logged.</li>
 * </ul>
 *
 * <p>The following communication events can be specified.  This is a bit mask
 * so multiple events can be enabled at the same time.  The log level must be
 * DEBUG or INFO for communication events to be logged.</p>
 * <ul>
 * <li>EXCEPTION  - Log HTTP exceptions.</li>
 * <li>HTTP-ERROR - Log non-200 HTTP responses.</li>
 * <li>HTTP-OK    - Log HTTP 200 responses.</li>
 * </ul>
 */

@Vetoed
public class SetLogging extends AbstractAPIRequestHandler {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SetLogging.class);

    /** Logging updated */
    private static final JSONStreamAware LOGGING_UPDATED;
    static {
        JSONObject response = new JSONObject();
        response.put("loggingUpdated", true);
        LOGGING_UPDATED = JSON.prepare(response);
    }

    /** Incorrect log level */
    private static final JSONStreamAware INCORRECT_LEVEL =
            JSONResponses.incorrect("logLevel", "Log level must be DEBUG, INFO, WARN or ERROR");

    /** Incorrect communication event */
    private static final JSONStreamAware INCORRECT_EVENT =
            JSONResponses.incorrect("communicationEvent",
                                    "Communication event must be EXCEPTION, HTTP-ERROR or HTTP-OK");

    /**
     * Create the SetLogging instance
     */
    public SetLogging() {
        super(new APITag[] {APITag.DEBUG}, "logLevel", "communicationEvent", "communicationEvent", "communicationEvent");
    }

    /**
     * Process the SetLogging API request
     *
     * @param   req                 API request
     * @return                      API response
     */
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        JSONStreamAware response = null;
        //
        // Get the log level
        //
        String value = req.getParameter("logLevel");
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (value != null) {
            switch (value) {
                case "DEBUG":
                    root.setLevel(Level.DEBUG);
                    break;
                case "INFO":
                    root.setLevel(Level.INFO);
                    break;
                case "WARN":
                    root.setLevel(Level.WARN);
                    break;
                case "ERROR":
                    root.setLevel(Level.ERROR);
                    break;
                default:
                    response = INCORRECT_LEVEL;
            }
        } else {
            root.setLevel(Level.INFO);
        }
        //
        // Get the communication events
        //
        if (response == null) {
            String[] events = req.getParameterValues("communicationEvent");
            if (!Peers.setCommunicationLoggingMask(events))
                response = INCORRECT_EVENT;
        }
        //
        // Return the response
        //
        if (response == null)
            response = LOGGING_UPDATED;
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
    protected final boolean requirePost() {
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
