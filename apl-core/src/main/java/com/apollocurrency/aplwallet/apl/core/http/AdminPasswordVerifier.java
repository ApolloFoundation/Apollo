/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.ParameterException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.LOCKED_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NO_PASSWORD_IN_CONFIG;

/**
 * @author al
 */
@Slf4j
@Singleton
public class AdminPasswordVerifier {
    public static final String ADMIN_ROLE = "admin";
    public static final String ADMIN_PASSWORD_PARAMETER_NAME = "adminPassword";
    private final Random random = new Random();
    private final String adminPassword;
    @Getter
    private final boolean disabledAdminPassword;
    private final Map<String, PasswordCount> incorrectPasswords = new HashMap<>();
    private final TimeService timeService;
    @Getter
    private final String forwardedForHeader;

    @Inject
    public AdminPasswordVerifier(PropertiesHolder propertiesHolder, TimeService timeService) {
        this.timeService = timeService;
        adminPassword = propertiesHolder.getStringProperty("apl.adminPassword", "", true);
        String host = propertiesHolder.getStringProperty("apl.apiServerHost");
        disabledAdminPassword = propertiesHolder.getBooleanProperty("apl.disableAdminPassword") || (isLocalHost(host) && adminPassword.isEmpty());
        forwardedForHeader = propertiesHolder.getStringProperty("apl.forwardedForHeader");
    }

    public boolean isBlankAdminPassword() {
        return adminPassword.isBlank();
    }

    public void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (disabledAdminPassword) {
            return;
        }
        if (isBlankAdminPassword()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        }
        int validationResult = checkOrLockPassword(req);
        switch (validationResult) {
            case 1:
                throw new ParameterException(LOCKED_ADMIN_PASSWORD);
            case 2:
                throw new ParameterException(INCORRECT_ADMIN_PASSWORD);
            case 3:
                throw new ParameterException(MISSING_ADMIN_PASSWORD);
            default:
        }
    }

    public Response verifyPasswordWithoutException(HttpServletRequest req) {
        if (disabledAdminPassword) {
            return null;
        }
        if (isBlankAdminPassword()) {
            return ResponseBuilder.apiError(ApiErrors.NO_PASSWORD_IN_CONFIG).build();
        }
        int validationResult = checkOrLockPassword(req);
        switch (validationResult) {
            case 1:
                return ResponseBuilder.apiError(ApiErrors.INCORRECT_PARAM, ADMIN_PASSWORD_PARAMETER_NAME, "locked for 1 hour, too many incorrect password attempts").build();
            case 2:
                return ResponseBuilder.apiError(ApiErrors.INCORRECT_PARAM, ADMIN_PASSWORD_PARAMETER_NAME, "the specified password does not match apl.adminPassword").build();
            case 3:
                return ResponseBuilder.apiError(ApiErrors.MISSING_PARAM, ADMIN_PASSWORD_PARAMETER_NAME).build();
            default:
                return null;
        }
    }

    public boolean checkPassword(HttpServletRequest req) {
        Response response = verifyPasswordWithoutException(req);
        return response == null;
    }

    /**
     * Validate password from req
     *
     * @param req request, which should be validated
     * @return 0, if password is valid, 1 - if too many attemps, 2 - when password is incorrect, 3 - when password is missing
     */
    private int checkOrLockPassword(HttpServletRequest req) {
        String remoteHost = null;
        if (forwardedForHeader != null) {
            remoteHost = req.getHeader(forwardedForHeader);
        }
        if (remoteHost == null) {
            remoteHost = req.getRemoteHost();
        }

        return checkOrLockPassword(req.getParameter(ADMIN_PASSWORD_PARAMETER_NAME), remoteHost);
    }

    public int checkOrLockPassword(String adminPasswordParam, String remoteHost) {
        int now = timeService.getEpochTime();
        String password = Convert.nullToEmpty(adminPasswordParam);

        synchronized (incorrectPasswords) {
            PasswordCount passwordCount = incorrectPasswords.get(remoteHost);
            if (passwordCount != null && passwordCount.count >= 25 && now - passwordCount.time < 60 * 60) {
                log.warn("Too many incorrect admin password attempts from " + remoteHost);
                return 1;
            }
            if (!password.equals(this.adminPassword)) {
                if (password.length() > 0) {
                    if (incorrectPasswords.size() > 1000) {
                        // Remove one of the locked hosts at random to prevent unlimited growth of the map
                        List<String> remoteHosts = new ArrayList<>(incorrectPasswords.keySet());
                        incorrectPasswords.remove(remoteHosts.get(random.nextInt(remoteHosts.size())));
                    }
                    if (passwordCount == null) {
                        passwordCount = new PasswordCount();
                    }
                    passwordCount.count++;
                    passwordCount.time = now;
                    incorrectPasswords.put(remoteHost, passwordCount);
                    log.warn("Incorrect adminPassword from " + remoteHost);
                    return 2;
                } else {
                    return 3;
                }
            }
            if (passwordCount != null) {
                incorrectPasswords.remove(remoteHost);
            }
        }
        return 0;
    }

    private boolean isLocalHost(String host) {
        return host != null && (host.equalsIgnoreCase("localhost")
            || host.equals("127.0.0.1")
            || host.endsWith("0:1")
            || host.endsWith("::1"));
    }

    @Vetoed
    private static class PasswordCount {
        private int count;
        private int time;
    }

}
