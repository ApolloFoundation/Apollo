/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BlockEventSource implements org.eclipse.jetty.servlets.EventSource
    {
        private Emitter emitter;


        @Override
        public void onOpen(Emitter emitter) throws IOException
        {
            this.emitter = emitter;
            emitter.data("SSE data");
            new Thread(()-> {
                while (true) {
                    try {emitter.data(String.valueOf(System.currentTimeMillis()));
                        TimeUnit.SECONDS.sleep(5);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        public void emitEvent(String dataToSend) throws IOException {
            this.emitter.data(dataToSend);
        }

        @Override
        public void onClose()
        {
        }
}
