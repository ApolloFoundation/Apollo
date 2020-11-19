/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@WebListener
public class ApiContextListener implements ServletContextListener {

    // TODO: YL remove static instance later
    private PropertiesHolder propertiesLoader;// = CDI.current().select(PropertiesHolder.class).get();

    public ApiContextListener() {
    }

    @Inject
    public ApiContextListener(PropertiesHolder propertiesLoader) {
        this.propertiesLoader = Objects.requireNonNull(propertiesLoader);
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        // create the thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            propertiesLoader.getIntProperty("apl.sseThreadPoolMinSize", 50),
            propertiesLoader.getIntProperty("apl.sseThreadPoolMaxSize", 200), 60000L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
        servletContextEvent.getServletContext().setAttribute("executor",
            executor);

    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) servletContextEvent
            .getServletContext().getAttribute("executor");
        executor.shutdown();
    }

}
