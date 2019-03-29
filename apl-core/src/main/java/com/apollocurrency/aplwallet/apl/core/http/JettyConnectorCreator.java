/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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

    private PropertiesHolder propertiesHolder;

    @Inject
    public JettyConnectorCreator(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public boolean addHttpConnector(String host, int port, Server apiServer, int idleTimeout  ) {
        ServerConnector connector = null;
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

    public boolean addHttpSConnector(String host, int port, Server apiServer, int idleTimeout ) {

        boolean res = true;
        ServerConnector connector = null;
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSendDateHeader(false);
        https_config.setSendServerVersion(false);
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
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
