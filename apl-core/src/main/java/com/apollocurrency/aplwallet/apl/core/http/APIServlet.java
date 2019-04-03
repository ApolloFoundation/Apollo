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

package com.apollocurrency.aplwallet.apl.core.http;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_DISABLED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_NOT_ALLOWED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.LIGHT_CLIENT_DISABLED_API;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.POST_REQUIRED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.REQUIRED_BLOCK_NOT_FOUND;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.REQUIRED_LAST_BLOCK_NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.addons.AddOns;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Helper2FA;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

public final class APIServlet extends HttpServlet {
    private static final Logger LOG = getLogger(APIServlet.class);

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();    
    private static final boolean enforcePost = propertiesHolder.getBooleanProperty("apl.apiServerEnforcePOST");
    public static final Map<String, AbstractAPIRequestHandler> apiRequestHandlers;
    public static final Map<String, AbstractAPIRequestHandler> disabledRequestHandlers;
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();

    static {

        Map<String, AbstractAPIRequestHandler> map = new HashMap<>();
        Map<String, AbstractAPIRequestHandler> disabledMap = new HashMap<>();

        for (APIEnum api : APIEnum.values()) {
            if (!api.getName().isEmpty() && api.getHandler() != null) {
                map.put(api.getName(), api.getHandler());
            }
        }

        AddOns.registerAPIRequestHandlers(map);

        API.disabledAPIs.forEach(api -> {
            AbstractAPIRequestHandler handler = map.remove(api);
            if (handler == null) {
                throw new RuntimeException("Invalid API in apl.disabledAPIs: " + api);
            }
            disabledMap.put(api, handler);
        });
        API.disabledAPITags.forEach(apiTag -> {
            Iterator<Map.Entry<String, AbstractAPIRequestHandler>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, AbstractAPIRequestHandler> entry = iterator.next();
                if (entry.getValue().getAPITags().contains(apiTag)) {
                    disabledMap.put(entry.getKey(), entry.getValue());
                    iterator.remove();
                }
            }
        });
        if (!API.disabledAPIs.isEmpty()) {
            LOG.info("Disabled APIs: " + API.disabledAPIs);
        }
        if (!API.disabledAPITags.isEmpty()) {
            LOG.info("Disabled APITags: " + API.disabledAPITags);
        }

        apiRequestHandlers = Collections.unmodifiableMap(map);
        disabledRequestHandlers = disabledMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(disabledMap);
    }

    public static AbstractAPIRequestHandler getAPIRequestHandler(String requestType) {
        return apiRequestHandlers.get(requestType);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Set response values now in case we create an asynchronous context
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/plain; charset=UTF-8");

        JSONStreamAware response = JSON.emptyJSON;
        long startTime = System.currentTimeMillis();
        boolean logRequestTime = false;
        try {

            if (!API.isAllowed(req.getRemoteHost())) {
                response = ERROR_NOT_ALLOWED;
                return;
            }

            String requestType = req.getParameter("requestType");
            if (requestType == null) {
                response = ERROR_INCORRECT_REQUEST;
                return;
            }

            AbstractAPIRequestHandler apiRequestHandler = apiRequestHandlers.get(requestType);
            if (apiRequestHandler == null) {
                if (disabledRequestHandlers.containsKey(requestType)) {
                    response = ERROR_DISABLED;
                } else {
                    response = ERROR_INCORRECT_REQUEST;
                }
                return;
            }

            if (propertiesHolder.isLightClient() && apiRequestHandler.requireFullClient()) {
                response = LIGHT_CLIENT_DISABLED_API;
                return;
            }

            if (enforcePost && apiRequestHandler.requirePost() && !"POST".equals(req.getMethod())) {
                response = POST_REQUIRED;
                return;
            }

            if (apiRequestHandler.requirePassword()) {
                API.verifyPassword(req);
            }
            String accountName2FA = apiRequestHandler.vaultAccountName();
            if (apiRequestHandler.is2FAProtected()) {
                Helper2FA.verify2FA(req, accountName2FA);
            }
            final long requireBlockId = apiRequestHandler.allowRequiredBlockParameters() ?
                    ParameterParser.getUnsignedLong(req, "requireBlock", false) : 0;
            final long requireLastBlockId = apiRequestHandler.allowRequiredBlockParameters() ?
                    ParameterParser.getUnsignedLong(req, "requireLastBlock", false) : 0;
            if (requireBlockId != 0 || requireLastBlockId != 0) {
                blockchain.readLock();
            }
            try {
                TransactionalDataSource dataSource = databaseManager.getDataSource();
                try {
                    if (apiRequestHandler.startDbTransaction()) {
                        dataSource.begin();
                    }
                    if (requireBlockId != 0 && !blockchain.hasBlock(requireBlockId)) {
                        response = REQUIRED_BLOCK_NOT_FOUND;
                        return;
                    }
                    if (requireLastBlockId != 0 && requireLastBlockId != blockchain.getLastBlock().getId()) {
                        response = REQUIRED_LAST_BLOCK_NOT_FOUND;
                        return;
                    }
                    response = apiRequestHandler.processRequest(req, resp);
                    logRequestTime = apiRequestHandler.logRequestTime();
                    if (requireLastBlockId == 0 && requireBlockId != 0 && response instanceof JSONObject) {
                        ((JSONObject) response).put("lastBlock", blockchain.getLastBlock().getStringId());
                    }
                } finally {
                    if (apiRequestHandler.startDbTransaction()) {
                        dataSource.commit(true);
                    }
                }
            } finally {
                if (requireBlockId != 0 || requireLastBlockId != 0) {
                    blockchain.readUnlock();
                }
            }
        } catch (ParameterException e) {
            response = e.getErrorResponse();
        } catch (AplException | RuntimeException e) {
            LOG.debug("Error processing API request", e);
            JSONObject json = new JSONObject();
            JSONData.putException(json, e);
            response = JSON.prepare(json);
        } catch (ExceptionInInitializerError err) {
            LOG.error("Initialization Error", err.getCause());
            response = ERROR_INCORRECT_REQUEST;
        } catch (Exception e) {
            LOG.error("Error processing request", e);
            response = ERROR_INCORRECT_REQUEST;
        } finally {
            // The response will be null if we created an asynchronous context
            if (response != null) {
                if (response instanceof JSONObject) {
                    long requestTime = System.currentTimeMillis() - startTime;
                    ((JSONObject) response).put("requestProcessingTime", requestTime);
                    if (logRequestTime) {
                        LOG.debug("Request \'" +req.getParameter("requestType")+ "\' took " + requestTime + " ms");
                    }
                }
                try (Writer writer = resp.getWriter()) {
                    JSON.writeJSONString(response, writer);
                }
            }
        }

    }

}
