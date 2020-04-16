/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.web;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.weld.environment.servlet.Listener;
import org.jboss.weld.module.web.servlet.WeldInitialListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

@Singleton
public class JettyServer {
    private Server server;

    public JettyServer() {
        server = new Server();
        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setSendDateHeader(false);
        configuration.setSendServerVersion(false);
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(configuration));
        connector.setPort(7870);
        connector.setHost("0.0.0.0");
        connector.setReuseAddress(true);
        server.addConnector(connector);
        ServletContextHandler servletHandler = new ServletContextHandler();
        // --------- ADD REST support servlet (RESTEasy)
        ServletHolder restEasyServletHolder = new ServletHolder(new HttpServletDispatcher());
        restEasyServletHolder.setInitParameter("resteasy.servlet.mapping.prefix", "/rest");
        restEasyServletHolder.setInitParameter("resteasy.injector.factory", "org.jboss.resteasy.cdi.CdiInjectorFactory");

        String restEasyAppClassName = RestEasyApplication.class.getName();
        restEasyServletHolder.setInitParameter("javax.ws.rs.Application", restEasyAppClassName);
        servletHandler.addServlet(restEasyServletHolder, "/rest/*");
        // init Weld here
        servletHandler.addEventListener(new WeldInitialListener());
        //need this listener to support scopes properly
        servletHandler.addEventListener(new Listener());

        server.setHandler(servletHandler);
    }

    @PostConstruct
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
