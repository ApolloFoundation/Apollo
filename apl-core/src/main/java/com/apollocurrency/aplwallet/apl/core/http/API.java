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

import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ClientErrorExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ConstraintViolationExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.DefaultGlobalExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.IllegalArgumentExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.LegacyParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiProtectionFilter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.ApiSplitFilter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.CharsetRequestFilter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FAInterceptor;
import com.apollocurrency.aplwallet.apl.core.rest.filters.SecurityInterceptor;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.api.provider.ByteArrayConverterProvider;
import com.apollocurrency.aplwallet.apl.util.api.provider.PlatformSpecConverterProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.weld.environment.servlet.Listener;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.MultipartConfigElement;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
@Slf4j
public final class API {

    private static final Logger LOG = getLogger(API.class);
    private static final String[] DISABLED_HTTP_METHODS = {"TRACE", "OPTIONS", "HEAD"};
    public static final String DEFAULT_WEBUI_DIR = "apollo-web-ui";
    public static final String INDEX_HTML = "index.html";
    public static int openAPIPort;
    public static int openAPISSLPort;
    public static boolean isOpenAPI;
    public static List<String> disabledAPIs;
    public static List<APITag> disabledAPITags;
    public static int maxRecords;
    public static int apiServerIdleTimeout;
    public static boolean apiServerCORS;
    //TODO: remove statics after switch to RestEasy handlers
    static PropertiesHolder propertiesHolder;
    static boolean enableAPIUPnP;
    private static Set<String> allowedBotHosts;
    private static List<NetworkAddress> allowedBotNets;
    private static Server apiServer;

    private static URI welcomePageUri;
    private static URI serverRootUri;
    private static List<Integer> externalPorts = new ArrayList<>();
    final int port;
    final int sslPort;
    final String host;
    final boolean enableAPIServer;
    final int maxThreadPoolSize;
    final int minThreadPoolSize;
    final boolean enableSSL;
    private final UPnP upnp;
    private final JettyConnectorCreator jettyConnectorCreator;

    @Inject
    public API(PropertiesHolder propertiesHolder, UPnP upnp, JettyConnectorCreator jettyConnectorCreator) {
        this.propertiesHolder = propertiesHolder;
        this.upnp = upnp;
        this.jettyConnectorCreator = jettyConnectorCreator;
        maxRecords = propertiesHolder.getIntProperty("apl.maxAPIRecords");
        enableAPIUPnP = propertiesHolder.getBooleanProperty("apl.enableAPIUPnP");
        apiServerIdleTimeout = propertiesHolder.getIntProperty("apl.apiServerIdleTimeout");
        apiServerCORS = propertiesHolder.getBooleanProperty("apl.apiServerCORS");
        //
        List<String> disabled = new ArrayList<>(propertiesHolder.getStringListProperty("apl.disabledAPIs"));
        Collections.sort(disabled);
        disabledAPIs = Collections.unmodifiableList(disabled);
        disabled = propertiesHolder.getStringListProperty("apl.disabledAPITags");
        Collections.sort(disabled);
        List<APITag> apiTags = new ArrayList<>(disabled.size());
        disabled.forEach(tagName -> apiTags.add(APITag.fromDisplayName(tagName)));
        disabledAPITags = Collections.unmodifiableList(apiTags);
        List<String> allowedBotHostsList = propertiesHolder.getStringListProperty("apl.allowedBotHosts");
        if (!allowedBotHostsList.contains("*")) {
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
//
        port = propertiesHolder.getIntProperty("apl.apiServerPort");
        sslPort = propertiesHolder.getIntProperty("apl.apiServerSSLPort");
        host = propertiesHolder.getStringProperty("apl.apiServerHost");
        enableAPIServer = propertiesHolder.getBooleanProperty("apl.enableAPIServer");
        maxThreadPoolSize = propertiesHolder.getIntProperty("apl.threadPoolMaxSize");
        minThreadPoolSize = propertiesHolder.getIntProperty("apl.threadPoolMinSize");
        enableSSL = propertiesHolder.getBooleanProperty("apl.apiSSL");
//
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
    }


    private static boolean isWebUIHere(Path webUiPath) {
        boolean res = false;
        if (Files.exists(webUiPath)
            && Files.isDirectory(webUiPath)
            && Files.exists(webUiPath.resolve(INDEX_HTML))) {
            log.debug("Web UI index.html foind in: {}.", webUiPath.toString());
            res = true;
        }
        return res;
    }

    public static String findWebUiDir() {
        final Path binDir = DirProvider.getBinDir();
        boolean useHtmlStub = true;
        final String webUIlocation = propertiesHolder.getStringProperty("apl.apiResourceBase", DEFAULT_WEBUI_DIR);
        Path webUiPath = Path.of(DEFAULT_WEBUI_DIR);
        try {
            Path lp = Path.of(webUIlocation);
            if (lp.isAbsolute()) {
                webUiPath = lp;
                if (isWebUIHere(webUiPath)) {
                    log.debug("Cannot find index.html in: {}. Gonna use html-stub.", webUiPath.toString());
                    useHtmlStub = false;
                }
            } else {
                webUiPath = binDir.resolve(webUIlocation);
                if (isWebUIHere(webUiPath)) {
                    useHtmlStub = false;
                } else {
                    webUiPath = binDir.getParent().resolve(webUIlocation);
                    if (isWebUIHere(webUiPath)) {
                        useHtmlStub = false;
                    }
                }
            }
        } catch (InvalidPathException ipe) {
            log.debug("Cannot resolve apl.webUIDir: {} within DirProvider.getBinDir(): {}. Gonna use html-stub.", webUIlocation, binDir.toString());
        }

        if (useHtmlStub) {
            webUiPath = binDir.resolve("html-stub").toAbsolutePath();
            if (Files.exists(webUiPath.resolve(INDEX_HTML))) {
                log.debug("webUIDir: {}", webUiPath.toString());
            } else {
                log.error("Cannot find dir with index.html: {}. Gonna proceed without any html-stub.", webUiPath);
            }
        }

        return webUiPath.toString();
    }

    public static boolean isAllowed(String remoteHost) {
        if (allowedBotHosts == null || allowedBotHosts.contains(remoteHost)) {
            return true;
        }
        try {
            BigInteger hostAddressToCheck = new BigInteger(InetAddress.getByName(remoteHost).getAddress());
            for (NetworkAddress network : allowedBotNets) {
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

    public static URI getWelcomePageUri() {
        return welcomePageUri;
    }

    @SneakyThrows
    public final void start() {

        if (enableAPIServer) {

            final QueuedThreadPool threadPool = getQueuedThreadPool();
            apiServer = new Server(threadPool);

            //
            // Create the HTTP connector
            //
            if (!enableSSL || port != sslPort) {
                jettyConnectorCreator.addHttpConnector(host, port, apiServer, apiServerIdleTimeout);
                LOG.info("API server using HTTP port " + port);
            }
            //
            // Create the HTTPS connector
            //

            if (enableSSL) {
                jettyConnectorCreator.addHttpSConnector(host, sslPort, apiServer, apiServerIdleTimeout);
            }

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = findWebUiDir();
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
            //add Weld listener
            apiHandler.addEventListener(new Listener());
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
            gzipHandler.setMinGzipSize(PeersService.MIN_COMPRESS_SIZE);
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
            //restEasyServletHolder.setInitParameter("resteasy.role.based.security", "true");

            restEasyServletHolder.setInitParameter(ResteasyContextParameters.RESTEASY_PROVIDERS,
                new StringJoiner(",")
                    .add(ConstraintViolationExceptionMapper.class.getName())
                    .add(ClientErrorExceptionMapper.class.getName())
                    .add(ParameterExceptionMapper.class.getName())
                    .add(LegacyParameterExceptionMapper.class.getName())
                    .add(SecurityInterceptor.class.getName())
                    .add(Secured2FAInterceptor.class.getName())
                    .add(RestParameterExceptionMapper.class.getName())
                    .add(DefaultGlobalExceptionMapper.class.getName())
                    .add(CharsetRequestFilter.class.getName())
                    .add(IllegalArgumentExceptionMapper.class.getName())
                    .add(PlatformSpecConverterProvider.class.getName())
                    .add(ByteArrayConverterProvider.class.getName())
                    .toString()
            );

            String restEasyAppClassName = RestEasyApplication.class.getName();
            restEasyServletHolder.setInitParameter("javax.ws.rs.Application", restEasyAppClassName);
            apiHandler.addServlet(restEasyServletHolder, "/rest/*");
            // init Weld here
            apiHandler.addEventListener(new org.jboss.weld.module.web.servlet.WeldInitialListener());
            //need this listener to support scopes properly
            apiHandler.addEventListener(new org.jboss.weld.environment.servlet.Listener());

            //--------- ADD swagger/openApi generated docs and API test page
            // Set the path to our static (Swagger UI) resources
            URL su = API.class.getResource("/swaggerui");
            if (su != null) {
                LOG.info("Swagger UI html/js resources base path= {}", su.toURI().toString());
                String resourceBasePath = su.toExternalForm();
                ContextHandler contextHandler = new ContextHandler("/swagger");
                ResourceHandler swFileHandler = new ResourceHandler();
                swFileHandler.setDirectoriesListed(false);
                swFileHandler.setWelcomeFiles(new String[]{INDEX_HTML});
                swFileHandler.setResourceBase(resourceBasePath);
                contextHandler.setHandler(swFileHandler);
                apiHandlers.addHandler(contextHandler);
            } else {
                LOG.warn("Swagger html/js resources not found, swagger UI is off.");
            }

            apiHandlers.addHandler(apiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.addBean(new APIErrorHandler());
            apiServer.setStopAtShutdown(true);
//            Log.getRootLogger().setDebugEnabled(true);

            try {

                if (enableAPIUPnP && upnp.isAvailable()) {
                    Connector[] apiConnectors = apiServer.getConnectors();
                    for (Connector apiConnector : apiConnectors) {
                        if (apiConnector instanceof ServerConnector) {
                            externalPorts.add(upnp.addPort(((ServerConnector) apiConnector).getPort(), "API"));
                        }
                    }
                    if (!externalPorts.isEmpty()) {
                        openAPIPort = externalPorts.get(0);
                    }
                }

                apiServer.start();

                LOG.info("Started API server at " + host + ":" + port + (enableSSL && port != sslPort ? ", " + host + ":" + sslPort : ""));
            } catch (Exception e) {
                LOG.error("Failed to start API server", e);
                throw new RuntimeException(e.toString(), e);
            }
            //  }, true);

        } else {
            apiServer = null;
            openAPIPort = 0;
            openAPISSLPort = 0;
            isOpenAPI = false;
            LOG.info("API server not enabled");
        }

    }

    private QueuedThreadPool getQueuedThreadPool() {
        int minThreadPoolSizeLocal;
        int maxThreadPoolSizeLocal;
        if (propertiesHolder.getBooleanProperty("apl.limitHardwareResources", false)) {
            minThreadPoolSizeLocal = propertiesHolder.getIntProperty("apl.apiMinThreadPoolSize");
            maxThreadPoolSizeLocal = propertiesHolder.getIntProperty("apl.apiMaxThreadPoolSize");
        } else {
            minThreadPoolSizeLocal = Math.max(minThreadPoolSize, 8);
            maxThreadPoolSizeLocal = Math.max(maxThreadPoolSize, 200);
        }
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(maxThreadPoolSizeLocal);
        threadPool.setMinThreads(minThreadPoolSizeLocal);
        threadPool.setName("APIThreadPool");
        return threadPool;
    }

    public final void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
                for (int extPort : externalPorts) {
                    upnp.deletePort(extPort);
                }
            } catch (Exception e) {
                LOG.info("Failed to stop API server", e);
            }
        }
    }

    private void disableHttpMethods(ServletContextHandler servletContext) {
        SecurityHandler securityHandler = servletContext.getSecurityHandler();
        if (securityHandler == null) {
            securityHandler = new ConstraintSecurityHandler();
            servletContext.setSecurityHandler(securityHandler);
        }
        disableHttpMethods(securityHandler);
    }

    private void disableHttpMethods(SecurityHandler securityHandler) {
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

    private void disableHttpMethod(ConstraintSecurityHandler securityHandler, String httpMethod) {
        ConstraintMapping mapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraint.setName("Disable " + httpMethod);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/");
        mapping.setMethod(httpMethod);
        securityHandler.addConstraintMapping(mapping);
    }

    public URI getServerRootUri() {
        return serverRootUri;
    }

}
