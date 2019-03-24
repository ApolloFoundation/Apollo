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

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.LOCKED_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_ADMIN_PASSWORD;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NO_PASSWORD_IN_CONFIG;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ConstraintViolationExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiProtectionFilter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiSplitFilter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Vetoed
public final class API {
    private static final Logger LOG = getLogger(API.class);

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static BlockchainConfig blockchainConfig;// = CDI.current().select(BlockchainConfig.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    private static final String[] DISABLED_HTTP_METHODS = {"TRACE", "OPTIONS", "HEAD"};
    private static byte[] privateKey;
    private static byte[] publicKey;
    private static FBElGamalKeyPair elGamalKeyPair;    
    public static int openAPIPort;
    public static int openAPISSLPort;
    public static boolean isOpenAPI;
    public static List<String> disabledAPIs;
    public static List<APITag> disabledAPITags;

    private static Set<String> allowedBotHosts;
    private static List<NetworkAddress> allowedBotNets;
    private static final Map<String, PasswordCount> incorrectPasswords = new HashMap<>();
    public static final String adminPassword = propertiesHolder.getStringProperty("apl.adminPassword", "", true);
    public static boolean disableAdminPassword;
    public static final int maxRecords = propertiesHolder.getIntProperty("apl.maxAPIRecords");
    static final boolean enableAPIUPnP = propertiesHolder.getBooleanProperty("apl.enableAPIUPnP");
    public static final int apiServerIdleTimeout = propertiesHolder.getIntProperty("apl.apiServerIdleTimeout");
    public static final boolean apiServerCORS = propertiesHolder.getBooleanProperty("apl.apiServerCORS");
    private static final String forwardedForHeader = propertiesHolder.getStringProperty("apl.forwardedForHeader");

    private static Server apiServer;

    private static URI welcomePageUri;
    private static URI serverRootUri;
    //TODO: remove static context
    private static final UPnP upnp = CDI.current().select(UPnP.class).get();

//TODO: remove this as soon as Al Gamal is ready!    
    private static Thread serverKeysGenerator = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (API.class) {
                byte[] keyBytes = new byte[32];
                Crypto.getSecureRandom().nextBytes(keyBytes);
                byte[] keySeed = Crypto.getKeySeed(keyBytes);
                privateKey = Crypto.getPrivateKey(keySeed);
                publicKey = Crypto.getPublicKey(keySeed);
                
                elGamalKeyPair = Crypto.getElGamalKeyPair();
                
            }
            try {
                TimeUnit.MINUTES.sleep(15);
            }
            catch (InterruptedException e) {
                return;
            }
        }
    });

    public static synchronized byte[] getServerPublicKey() {
        return publicKey;
    }
    public static synchronized byte[] getServerPrivateKey() {
        return privateKey;
    }

    public static synchronized FBElGamalKeyPair getServerElGamalPublicKey() {
        
        return elGamalKeyPair;
        
    }
    
    public static String elGamalDecrypt(String cryptogramm)
    {
        return Crypto.elGamalDecrypt(cryptogramm, elGamalKeyPair);
    }
    
    public static void init() {
//    static {
        serverKeysGenerator.setDaemon(true);
        List<String> disabled = new ArrayList<>(propertiesHolder.getStringListProperty("apl.disabledAPIs"));
        Collections.sort(disabled);
        disabledAPIs = Collections.unmodifiableList(disabled);
        disabled = propertiesHolder.getStringListProperty("apl.disabledAPITags");
        Collections.sort(disabled);
        List<APITag> apiTags = new ArrayList<>(disabled.size());
        disabled.forEach(tagName -> apiTags.add(APITag.fromDisplayName(tagName)));
        disabledAPITags = Collections.unmodifiableList(apiTags);
        List<String> allowedBotHostsList = propertiesHolder.getStringListProperty("apl.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            Set<String> hosts = new HashSet<>();
            List<NetworkAddress> nets = new ArrayList<>();
            for (String host : allowedBotHostsList) {
                if (host.contains("/")) {
                    try {
                        nets.add(new NetworkAddress(host));
                    } catch (UnknownHostException e) {
                        LOG.error("Unknown network " + host, e);
                        throw new RuntimeException(e.toString(), e);
                    }
                } else {
                    hosts.add(host);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(hosts);
            allowedBotNets = Collections.unmodifiableList(nets);
        } else {
            allowedBotHosts = null;
            allowedBotNets = null;
        }

        boolean enableAPIServer = propertiesHolder.getBooleanProperty("apl.enableAPIServer");
        if (blockchainConfig == null) blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        if (enableAPIServer) {

            final int port = propertiesHolder.getIntProperty("apl.apiServerPort");
            final int sslPort = propertiesHolder.getIntProperty("apl.apiServerSSLPort");
            final String host = propertiesHolder.getStringProperty("apl.apiServerHost");
            disableAdminPassword = propertiesHolder.getBooleanProperty("apl.disableAdminPassword") || ("127.0.0.1".equals(host) && adminPassword.isEmpty());
            int maxThreadPoolSize = propertiesHolder.getIntProperty("apl.threadPoolMaxSize");
            int minThreadPoolSize = propertiesHolder.getIntProperty("apl.threadPoolMinSize");
            org.eclipse.jetty.util.thread.QueuedThreadPool threadPool = new org.eclipse.jetty.util.thread.QueuedThreadPool();
            threadPool.setMaxThreads(Math.max(maxThreadPoolSize, 200));
            threadPool.setMinThreads(Math.max(minThreadPoolSize, 8));
            threadPool.setName("APIThreadPool");
            apiServer = new Server(threadPool);
            ServerConnector connector;
            boolean enableSSL = propertiesHolder.getBooleanProperty("apl.apiSSL");
            //
            // Create the HTTP connector
            //
            if (!enableSSL || port != sslPort) {
                HttpConfiguration configuration = new HttpConfiguration();
                configuration.setSendDateHeader(false);
                configuration.setSendServerVersion(false);

                connector = new ServerConnector(apiServer, new HttpConnectionFactory(configuration));
                connector.setPort(port);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                LOG.info("API server using HTTP port " + port);
            }
            //
            // Create the HTTPS connector
            //
            final SslContextFactory sslContextFactory;
            if (enableSSL) {
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSendDateHeader(false);
                https_config.setSendServerVersion(false);
                https_config.setSecureScheme("https");
                https_config.setSecurePort(sslPort);
                https_config.addCustomizer(new SecureRequestCustomizer());
                sslContextFactory = new SslContextFactory();
                String keyStorePath = Paths.get(AplCoreRuntime.getInstance().getUserHomeDir()).resolve(Paths.get(propertiesHolder.getStringProperty("apl.keyStorePath"))).toString();
                LOG.info("Using keystore: " + keyStorePath);
                sslContextFactory.setKeyStorePath(keyStorePath);
                sslContextFactory.setKeyStorePassword(propertiesHolder.getStringProperty("apl.keyStorePassword", null, true));
                sslContextFactory.addExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.addExcludeProtocols("SSLv3");
                sslContextFactory.setKeyStoreType(propertiesHolder.getStringProperty("apl.keyStoreType"));
                List<String> ciphers = propertiesHolder.getStringListProperty("apl.apiSSLCiphers");
                if (!ciphers.isEmpty()) {
                    sslContextFactory.setIncludeCipherSuites(ciphers.toArray(new String[ciphers.size()]));
                }
                connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(https_config));
                connector.setPort(sslPort);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                LOG.info("API server using HTTPS port " + sslPort);
            } else {
                sslContextFactory = null;
            }
            String localhost = "0.0.0.0".equals(host) || "127.0.0.1".equals(host) ? "localhost" : host;
            try {
                welcomePageUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "/", null, null);
                serverRootUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "", null, null);
            } catch (URISyntaxException e) {
                LOG.info("Cannot resolve browser URI", e);
            }
            openAPIPort = !propertiesHolder.isLightClient() && "0.0.0.0".equals(host) && allowedBotHosts == null && (!enableSSL || port != sslPort) ? port : 0;
            openAPISSLPort = !propertiesHolder.isLightClient() && "0.0.0.0".equals(host) && allowedBotHosts == null && enableSSL ? sslPort : 0;
            isOpenAPI = openAPIPort > 0 || openAPISSLPort > 0;

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = AplCoreRuntime.getInstance().findWebUiDir();
            if (apiResourceBase != null && !apiResourceBase.isEmpty()) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                defaultServletHolder.setInitParameter("etags", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                String[] wellcome = {propertiesHolder.getStringProperty("apl.apiWelcomeFile")};
                apiHandler.setWelcomeFiles(wellcome);
            }

            ServletHolder servletHolder = apiHandler.addServlet(APIServlet.class, "/apl");
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                    null, Math.max(propertiesHolder.getIntProperty("apl.maxUploadFileSize"), Constants.MAX_TAGGED_DATA_DATA_LENGTH), -1L, 0));

            servletHolder = apiHandler.addServlet(APIProxyServlet.class, "/apl-proxy");
            servletHolder.setInitParameters(Collections.singletonMap("idleTimeout",
                    "" + Math.max(apiServerIdleTimeout - APIProxyServlet.PROXY_IDLE_TIMEOUT_DELTA, 0)));
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                    null, Math.max(propertiesHolder.getIntProperty("apl.maxUploadFileSize"), Constants.MAX_TAGGED_DATA_DATA_LENGTH), -1L, 0));

            GzipHandler gzipHandler = new GzipHandler();
            if (!propertiesHolder.getBooleanProperty("apl.enableAPIServerGZIPFilter", isOpenAPI)) {
                gzipHandler.setExcludedPaths("/apl", "/apl-proxy");
            }
            gzipHandler.setIncludedMethods("GET", "POST");
            gzipHandler.setMinGzipSize(Peers.MIN_COMPRESS_SIZE);
            gzipHandler.addExcludedPaths("/blocks");
            apiHandler.setGzipHandler(gzipHandler);

            apiHandler.addServlet(APITestServlet.class, "/test");
            apiHandler.addServlet(APITestServlet.class, "/test-proxy");

            apiHandler.addServlet(BlockEventSourceServlet.class, "/blocks").setAsyncSupported(true);

//TODO: do we need it at all?
//            apiHandler.addServlet(DbShellServlet.class, "/dbshell");

            apiHandler.addEventListener(new ApiContextListener());
            // Filter to forward requests to new API
            {
              FilterHolder filterHolder = apiHandler.addFilter(ApiSplitFilter.class, "/*", null);
              filterHolder.setAsyncSupported(true);
              filterHolder = apiHandler.addFilter(ApiProtectionFilter.class, "/*", null);
              filterHolder.setAsyncSupported(true);
            }
            if (apiServerCORS) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            if (propertiesHolder.getBooleanProperty("apl.apiFrameOptionsSameOrigin")) {
                FilterHolder filterHolder = apiHandler.addFilter(XFrameOptionsFilter.class, "/*", null);
                filterHolder.setAsyncSupported(true);
            }
            disableHttpMethods(apiHandler);

            // --------- ADD REST support servlet (RESTEasy)
            ServletHolder restEasyServletHolder = new ServletHolder(new HttpServletDispatcher());
            restEasyServletHolder.setInitParameter("resteasy.servlet.mapping.prefix", "/rest");
            restEasyServletHolder.setInitParameter("resteasy.injector.factory", "org.jboss.resteasy.cdi.CdiInjectorFactory");

            restEasyServletHolder.setInitParameter(ResteasyContextParameters.RESTEASY_PROVIDERS,
                    new StringJoiner(",")
                            .add(ConstraintViolationExceptionMapper.class.getName())
                            .add(ParameterExceptionMapper.class.getName())
                            .add(RestParameterExceptionMapper.class.getName())
                            .toString()
            );

            String restEasyAppClassName = RestEasyApplication.class.getName();
            restEasyServletHolder.setInitParameter("javax.ws.rs.Application", restEasyAppClassName);
            apiHandler.addServlet(restEasyServletHolder, "/rest/*");
            // init Weld here
            apiHandler.addEventListener(new org.jboss.weld.module.web.servlet.WeldInitialListener());
            //need this listener to support scopes properly
            apiHandler.addEventListener( new org.jboss.weld.environment.servlet.Listener());

            //--------- ADD swagger generated docs and API test page
            // Set the path to our static (Swagger UI) resources

            URL su =  API.class.getResource("/swaggerui");
            if(su!=null){
                String resourceBasePath = su.toExternalForm();
                ContextHandler contextHandler = new ContextHandler("/swagger");
                ResourceHandler swFileHandler = new ResourceHandler();
                swFileHandler.setDirectoriesListed(false);
                swFileHandler.setWelcomeFiles(new String[]{"index.html"});
                swFileHandler.setResourceBase(resourceBasePath);
                contextHandler.setHandler(swFileHandler);
                apiHandlers.addHandler(contextHandler);
            }else{
                LOG.warn("Swagger html/js resources not found, swagger UI is off.");
            }


            apiHandlers.addHandler(apiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.addBean(new APIErrorHandler());
            apiServer.setStopAtShutdown(true);
//            Log.getRootLogger().setDebugEnabled(true);

//            ThreadPool.runBeforeStart("APIInitThread", () -> {
                try {
                    serverKeysGenerator.start();
                    if (enableAPIUPnP) {
                        Connector[] apiConnectors = apiServer.getConnectors();
                        for (Connector apiConnector : apiConnectors) {
                            if (apiConnector instanceof ServerConnector)
                                upnp.addPort(((ServerConnector)apiConnector).getPort());
                        }
                    }

                    apiServer.start();
                    if (sslContextFactory != null) {
                        LOG.debug("API SSL Protocols: " + Arrays.toString(sslContextFactory.getSelectedProtocols()));
                        LOG.debug("API SSL Ciphers: " + Arrays.toString(sslContextFactory.getSelectedCipherSuites()));
                    }
                    LOG.info("Started API server at " + host + ":" + port + (enableSSL && port != sslPort ? ", " + host + ":" + sslPort : ""));
                } catch (Exception e) {
                    LOG.error("Failed to start API server", e);
                    throw new RuntimeException(e.toString(), e);
                }

          //  }, true);

        } else {
            apiServer = null;
            disableAdminPassword = false;
            openAPIPort = 0;
            openAPISSLPort = 0;
            isOpenAPI = false;
            LOG.info("API server not enabled");
        }

    }

//    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
                if (enableAPIUPnP) {
                    Connector[] apiConnectors = apiServer.getConnectors();
                    for (Connector apiConnector : apiConnectors) {
                        if (apiConnector instanceof ServerConnector)
                            upnp.deletePort(((ServerConnector)apiConnector).getPort());
                    }
                }
            } catch (Exception e) {
                LOG.info("Failed to stop API server", e);
            }
        }
    }

    public static void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (API.disableAdminPassword) {
            return;
        }
        if (API.adminPassword.isEmpty()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        }
        checkOrLockPassword(req);
    }

    public static boolean checkPassword(HttpServletRequest req) {
        if (API.disableAdminPassword) {
            return true;
        }
        if (API.adminPassword.isEmpty()) {
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

    private static void checkOrLockPassword(HttpServletRequest req) throws ParameterException {
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
            if (!API.adminPassword.equals(adminPassword)) {
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

    static boolean isAllowed(String remoteHost) {
        if (API.allowedBotHosts == null || API.allowedBotHosts.contains(remoteHost)) {
            return true;
        }
        try {
            BigInteger hostAddressToCheck = new BigInteger(InetAddress.getByName(remoteHost).getAddress());
            for (NetworkAddress network : API.allowedBotNets) {
                if (network.contains(hostAddressToCheck)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // can't resolve, disallow
            LOG.info("Unknown remote host " + remoteHost);
        }
        return false;

    }

    private static void disableHttpMethods(ServletContextHandler servletContext) {
        SecurityHandler securityHandler = servletContext.getSecurityHandler();
        if (securityHandler == null) {
            securityHandler = new ConstraintSecurityHandler();
            servletContext.setSecurityHandler(securityHandler);
        }
        disableHttpMethods(securityHandler);
    }

    private static void disableHttpMethods(SecurityHandler securityHandler) {
        if (securityHandler instanceof ConstraintSecurityHandler) {
            ConstraintSecurityHandler constraintSecurityHandler = (ConstraintSecurityHandler) securityHandler;
            for (String method : DISABLED_HTTP_METHODS) {
                disableHttpMethod(constraintSecurityHandler, method);
            }
            ConstraintMapping enableEverythingButTraceMapping = new ConstraintMapping();
            Constraint enableEverythingButTraceConstraint = new Constraint();
            enableEverythingButTraceConstraint.setName("Enable everything but TRACE");
            enableEverythingButTraceMapping.setConstraint(enableEverythingButTraceConstraint);
            enableEverythingButTraceMapping.setMethodOmissions(DISABLED_HTTP_METHODS);
            enableEverythingButTraceMapping.setPathSpec("/");
            constraintSecurityHandler.addConstraintMapping(enableEverythingButTraceMapping);
        }
    }

    private static void disableHttpMethod(ConstraintSecurityHandler securityHandler, String httpMethod) {
        ConstraintMapping mapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraint.setName("Disable " + httpMethod);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/");
        mapping.setMethod(httpMethod);
        securityHandler.addConstraintMapping(mapping);
    }

    private static class NetworkAddress {

        private BigInteger netAddress;
        private BigInteger netMask;

        private NetworkAddress(String address) throws UnknownHostException {
            String[] addressParts = address.split("/");
            if (addressParts.length == 2) {
                InetAddress targetHostAddress = InetAddress.getByName(addressParts[0]);
                byte[] srcBytes = targetHostAddress.getAddress();
                netAddress = new BigInteger(1, srcBytes);
                int maskBitLength = Integer.valueOf(addressParts[1]);
                int addressBitLength = (targetHostAddress instanceof Inet4Address) ? 32 : 128;
                netMask = BigInteger.ZERO
                        .setBit(addressBitLength)
                        .subtract(BigInteger.ONE)
                        .subtract(BigInteger.ZERO.setBit(addressBitLength - maskBitLength).subtract(BigInteger.ONE));
            } else {
                throw new IllegalArgumentException("Invalid address: " + address);
            }
        }

        private boolean contains(BigInteger hostAddressToCheck) {
            return hostAddressToCheck.and(netMask).equals(netAddress);
        }

    }

    public static final class XFrameOptionsFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }

    }

    public static URI getWelcomePageUri() {
        return welcomePageUri;
    }

    public static URI getServerRootUri() {
        return serverRootUri;
    }

    private API() {} // never

}
