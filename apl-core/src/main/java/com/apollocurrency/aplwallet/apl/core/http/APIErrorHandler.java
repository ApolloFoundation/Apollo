/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

public class APIErrorHandler extends ErrorPageErrorHandler {

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesLoader = CDI.current().select(PropertiesHolder.class).get();

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND){
            String apiResourceBase = AplCoreRuntime.getInstance().findWebUiDir();
//propertiesLoader.getStringProperty("apl.apiResourceBase");
            String apiWelcomePage = propertiesLoader.getStringProperty("apl.apiWelcomeFile");

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