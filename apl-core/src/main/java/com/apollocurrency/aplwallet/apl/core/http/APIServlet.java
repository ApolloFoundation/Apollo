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

import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.addons.AddOns;
import com.apollocurrency.aplwallet.apl.core.app.BlockNotFoundException;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_DISABLED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.ERROR_NOT_ALLOWED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.LIGHT_CLIENT_DISABLED_API;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.POST_REQUIRED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.REQUIRED_BLOCK_NOT_FOUND;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.REQUIRED_LAST_BLOCK_NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

public final class APIServlet extends HttpServlet {
    private static final Logger LOG = getLogger(APIServlet.class);
    public static Map<String, AbstractAPIRequestHandler> apiRequestHandlers;
    public static Map<String, AbstractAPIRequestHandler> disabledRequestHandlers;
    private final PropertiesHolder propertiesHolder;
    private final boolean enforcePost;
    private final Blockchain blockchain;//
    private final GlobalSync globalSync; // = CDI.current().select(GlobalSync.class).get();
    private final AdminPasswordVerifier apw; // =  CDI.current().select(AdminPasswordVerifier.class).get();
    private final Account2FAService account2FAService;

    @Inject
    public APIServlet() {
        this.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        this.blockchain = CDI.current().select(BlockchainImpl.class).get();
        this.globalSync = CDI.current().select(GlobalSync.class).get();
        this.apw = CDI.current().select(AdminPasswordVerifier.class).get();
        this.account2FAService = CDI.current().select(Account2FAService.class).get();


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
        enforcePost = propertiesHolder.getBooleanProperty("apl.apiServerEnforcePOST");
    }

    public static AbstractAPIRequestHandler getAPIRequestHandler(String requestType) {
        return apiRequestHandlers.get(requestType);
    }

    @Override
    public void init() {
        LOG.debug("API servlet init");
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
                apw.verifyPassword(req);
            }
            String accountName2FA = apiRequestHandler.vaultAccountName();
            if (apiRequestHandler.is2FAProtected()) {
                TwoFactorAuthParameters params2FA = HttpParameterParserUtil.parse2FARequest(req, accountName2FA, false);
                account2FAService.verify2FA(params2FA);
            }
            final long requireBlockId = apiRequestHandler.allowRequiredBlockParameters() ?
                HttpParameterParserUtil.getUnsignedLong(req, "requireBlock", false) : 0;
            final long requireLastBlockId = apiRequestHandler.allowRequiredBlockParameters() ?
                HttpParameterParserUtil.getUnsignedLong(req, "requireLastBlock", false) : 0;
            if (requireBlockId != 0 || requireLastBlockId != 0) {
                globalSync.readLock();
            }
            try {
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
                if (requireBlockId != 0 || requireLastBlockId != 0) {
                    globalSync.readUnlock();
                }
            }
        } catch (ParameterException e) {
            response = e.getErrorResponse();
        } catch (BlockNotFoundException e) {
            LOG.error("Error: {}", e.getMessage());
            LOG.debug("Trace: {}", ThreadUtils.lastStacktrace(e.getStackTrace(), 5));
            JSONObject json = new JSONObject();
            JSONData.putException(json, e);
            response = JSON.prepare(json);
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
                        LOG.debug("Request \'" + req.getParameter("requestType") + "\' took " + requestTime + " ms");
                    }
                }
                try (Writer writer = resp.getWriter()) {
                    JSON.writeJSONString(response, writer);
                }
            }
        }

    }

}
