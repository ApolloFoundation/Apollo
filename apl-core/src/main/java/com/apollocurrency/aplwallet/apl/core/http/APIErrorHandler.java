/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

public class APIErrorHandler extends ErrorPageErrorHandler {


    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
            String apiResourceBase = API.findWebUiDir();
//propertiesLoader.getStringProperty("apl.apiResourceBase");
            String apiWelcomePage = API.propertiesHolder.getStringProperty("apl.apiWelcomeFile");

            response.setContentType("text/html");
            response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
            response.setStatus(HttpServletResponse.SC_OK);

            PrintWriter out = response.getWriter();
            out.print(IOUtils.toString(new FileReader(apiResourceBase + "/" + apiWelcomePage)));
        } else {
            super.handle(target, baseRequest, request, response);
        }
    }
}
