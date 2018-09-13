/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet;

import org.slf4j.Logger;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.PrintWriter;

import static org.slf4j.LoggerFactory.getLogger;

@WebListener
public class AppAsyncListener implements AsyncListener {
        private static final Logger LOG = getLogger(AppAsyncListener.class);

    @Override
    public void onComplete(AsyncEvent asyncEvent) throws IOException {
        System.out.println("AppAsyncListener onComplete");
        // we can do resource cleanup activity here
    }

    @Override
    public void onError(AsyncEvent asyncEvent) throws IOException {
        LOG.trace("onErrorAsync {}", asyncEvent);
        LOG.error("error", asyncEvent.getThrowable());
        System.out.println("AppAsyncListener onError");
        //we can return error response to client
    }

    @Override
    public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
        LOG.trace("onStartAsync {}", asyncEvent);
        System.out.println("AppAsyncListener onStartAsync");
        //we can log the event here
    }

    @Override
    public void onTimeout(AsyncEvent asyncEvent) throws IOException {
        System.out.println("AppAsyncListener onTimeout");
        //we can send appropriate response to client
        ServletResponse response = asyncEvent.getAsyncContext().getResponse();
        PrintWriter out = response.getWriter();
        out.write("TimeOut Error in Processing");
    }

}