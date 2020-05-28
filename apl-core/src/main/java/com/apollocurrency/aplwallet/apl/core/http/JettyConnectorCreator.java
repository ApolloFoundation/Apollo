/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Paths;

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
    public JettyConnectorCreator(PropertiesHolder propertiesHolder, DirProvider dirProvider) {
        keyStorePath = Paths.get(dirProvider.getAppBaseDir().toString())
            .resolve(Paths.get(propertiesHolder.getStringProperty("apl.keyStorePath")))
            .toString();
        keyStorePassword = propertiesHolder.getStringProperty("apl.keyStorePassword", null, true);
        keyStoreAlias = propertiesHolder.getStringProperty("apl.keyStoreAlias", "jetty", true);
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
        String proto = connector.getDefaultProtocol();
        LOG.debug("API protocol: {} configured", proto);
        return true;
    }

    public boolean addHttpSConnector(String host, int port, Server apiServer, int idleTimeout) {

        ServerConnector connector;
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSendDateHeader(false);
        https_config.setSendServerVersion(false);
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        LOG.info("Using keystore: {} with alias: {} ", keyStorePath, keyStoreAlias);
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.addExcludeProtocols("SSLv3", "TLSv1", "TLSv1.1"); //TLS 1.2 or 1.3 only

        // we need to trust self-signed certificates in TLS but we do verify certificate
        // of node using Apollo's own CA on peer-ro-peer connection establishment
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setCertAlias(keyStoreAlias);
        //use Bouncy Castle provider
        sslContextFactory.setProvider("BC");
        SslConnectionFactory cf = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
        connector = new ServerConnector(apiServer, cf, new HttpConnectionFactory(https_config));
        connector.setPort(port);
        connector.setHost(host);
        connector.setIdleTimeout(idleTimeout);
        connector.setReuseAddress(true);
        apiServer.addConnector(connector);
        String proto = connector.getDefaultProtocol();
        LOG.debug("API protocol: {} configured", proto);
        return true;
    }
}
