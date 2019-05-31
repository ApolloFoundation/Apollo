/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.inject.Inject;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to prepare HTTP or HTTPS connectors for Jetty server
 *
 * @author alukin@gmail.com
 */
public class JettyConnectorCreator {

    private static final Logger LOG = LoggerFactory.getLogger(JettyConnectorCreator.class);

    private final String keyStoreAlias;
    private final String keyStorePath;
    private final String keyStorePassword;
    
    @Inject
    public JettyConnectorCreator(PropertiesHolder propertiesHolder) {
        keyStorePath = Paths.get(AplCoreRuntime.getInstance().getUserHomeDir())
                            .resolve(Paths.get(propertiesHolder.getStringProperty("apl.keyStorePath")))
                            .toString();
        keyStorePassword=propertiesHolder.getStringProperty("apl.keyStorePassword", null, true);
        keyStoreAlias=propertiesHolder.getStringProperty("apl.keyStoreAlias", "jetty", true);
    }

    public boolean addHttpConnector(String host, int port, Server apiServer, int idleTimeout) {
        ServerConnector connector;
        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setSendDateHeader(false);
        configuration.setSendServerVersion(false);

        connector = new ServerConnector(apiServer, new HttpConnectionFactory(configuration));
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(idleTimeout);
        connector.setReuseAddress(true);
        apiServer.addConnector(connector);
        return true;
    }

    public boolean addHttpSConnector(String host, int port, Server apiServer, int idleTimeout) {

        boolean res = true;
        ServerConnector connector;
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSendDateHeader(false);
        https_config.setSendServerVersion(false);
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        LOG.info("Using keystore: " + keyStorePath);
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.addExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        //TODO: try tp use TLSv1.3 only
        sslContextFactory.addExcludeProtocols("SSLv3","TLSv1","TLSv1.1"); //TLS 1.2 or 1.3 only

        // we need to trust self-signed certificates in TLS but we do verify cerificate
        // of node using Apollo's own CA on peer-ro-peer connection establishment
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setTrustManagerFactoryAlgorithm("TrustAll");
        //TODO: try to set other alias for certificate
        sslContextFactory.setCertAlias(keyStoreAlias);
        //TODO: try to use BC
        //sslContextFactory.setProvider("BC");
        connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https_config));
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(idleTimeout);
        connector.setReuseAddress(true);
        apiServer.addConnector(connector);
        LOG.debug("API SSL Protocols: " + Arrays.toString(sslContextFactory.getSelectedProtocols()));
        LOG.debug("API SSL Ciphers: " + Arrays.toString(sslContextFactory.getSelectedCipherSuites()));
        return res;
    }
}
