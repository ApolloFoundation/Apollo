/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.LOCKED_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NO_PASSWORD_IN_CONFIG;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author al
 */
@Singleton
public class AdminPasswordVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(AdminPasswordVerifier.class);
    private final PropertiesHolder propertiesHolder;
    public String adminPassword="";
    public final boolean disableAdminPassword;
    private final Map<String, PasswordCount> incorrectPasswords = new HashMap<>();
    private final TimeService timeService;
    private final String forwardedForHeader;
    
    @Inject
    public AdminPasswordVerifier(PropertiesHolder propertiesHolder, TimeService timeService) {
        this.propertiesHolder = propertiesHolder;
        this.timeService = timeService;
        
        adminPassword = propertiesHolder.getStringProperty("apl.adminPassword", "", true);
        String host = propertiesHolder.getStringProperty("apl.apiServerHost");
        disableAdminPassword = propertiesHolder.getBooleanProperty("apl.disableAdminPassword") || ("127.0.0.1".equals(host) && adminPassword.isEmpty());
        forwardedForHeader = propertiesHolder.getStringProperty("apl.forwardedForHeader");  
    }
    
    public void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (disableAdminPassword) {
            return;
        }
        if (adminPassword.isEmpty()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        }
        checkOrLockPassword(req);
    }

    public boolean checkPassword(HttpServletRequest req) {
        if (disableAdminPassword) {
            return true;
        }
        if (adminPassword.isEmpty()) {
            return false;
        }
        if (Convert.emptyToNull(req.getParameter("adminPassword")) == null) {
            return false;
        }
        try {
            checkOrLockPassword(req);
            return true;
        } catch (ParameterException e) {
            return false;
        }
    }

@Vetoed
    private static class PasswordCount {
        private int count;
        private int time;
    }

    private void checkOrLockPassword(HttpServletRequest req) throws ParameterException {
        int now = timeService.getEpochTime();
        String remoteHost = null;
        if (forwardedForHeader != null) {
            remoteHost = req.getHeader(forwardedForHeader);
        }
        if (remoteHost == null) {
            remoteHost = req.getRemoteHost();
        }
        synchronized(incorrectPasswords) {
            PasswordCount passwordCount = incorrectPasswords.get(remoteHost);
            if (passwordCount != null && passwordCount.count >= 25 && now - passwordCount.time < 60*60) {
                LOG.warn("Too many incorrect admin password attempts from " + remoteHost);
                throw new ParameterException(LOCKED_ADMIN_PASSWORD);
            }
            String adminPassword = Convert.nullToEmpty(req.getParameter("adminPassword"));
            if (!adminPassword.equals(this.adminPassword)) {
                if (adminPassword.length() > 0) {
                    if (passwordCount == null) {
                        passwordCount = new PasswordCount();
                        incorrectPasswords.put(remoteHost, passwordCount);
                        if (incorrectPasswords.size() > 1000) {
                            // Remove one of the locked hosts at random to prevent unlimited growth of the map
                            List<String> remoteHosts = new ArrayList<>(incorrectPasswords.keySet());
                            Random r = new Random();
                            incorrectPasswords.remove(remoteHosts.get(r.nextInt(remoteHosts.size())));
                        }
                    }
                    passwordCount.count++;
                    passwordCount.time = now;
                    LOG.warn("Incorrect adminPassword from " + remoteHost);
                    throw new ParameterException(INCORRECT_ADMIN_PASSWORD);
                } else {
                    throw new ParameterException(MISSING_ADMIN_PASSWORD);
                }
            }
            if (passwordCount != null) {
                incorrectPasswords.remove(remoteHost);
            }
        }
    }

}
