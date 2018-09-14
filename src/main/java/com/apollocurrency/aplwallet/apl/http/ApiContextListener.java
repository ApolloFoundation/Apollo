/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@WebListener
public class ApiContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        // create the thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(Apl.getIntProperty("apl.sseThreadPoolMinSize", 50), Apl.getIntProperty("apl.sseThreadPoolMaxSize", 200), 60000L,
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