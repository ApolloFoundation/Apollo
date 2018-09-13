/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class SSE extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter writer = null;
        try{
            res.setContentType("text/event-stream");
            res.setCharacterEncoding("UTF-8");
//            res.setHeader("Cache-Control", "no-cache,no-store,max-age=0,max-stale=0");
            res.setHeader("Connection", "close");
            writer = res.getWriter();
            int i = 6;

            while (--i > 0) {
                writer.print("data: " + System.currentTimeMillis() + " \n\n");
                res.flushBuffer();
                TimeUnit.SECONDS.sleep(2);
            }

        }catch(Exception ex){
            System.out.println("ECGDataUpdater:doGet :: " + ex.toString());
            ex.printStackTrace();
        }
        finally{
            try{
            }catch(Exception ex){
                ex.printStackTrace();
            }
            writer.close();
        }
    }
}
