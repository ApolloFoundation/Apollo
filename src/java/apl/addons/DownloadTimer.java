/*
* Copyright © 2013-2016 The Nxt Core Developers.
* Copyright © 2016-2017 Jelurida IP B.V.
*
* See the LICENSE.txt file at the top-level directory of this distribution
* for licensing information.
*
* Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
* no part of the Nxt software, including this file, may be copied, modified,
* propagated, or distributed except according to the terms contained in the
* LICENSE.txt file.
*
* Removal or modification of this copyright notice is prohibited.
*
*/

/*
* Copyright © 2018 Apollo Foundation
*/

package apl.addons;

import apl.Block;
import apl.BlockchainProcessor;
import apl.Apl;
import apl.util.Listener;
import apl.util.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

public final class DownloadTimer implements AddOn {

    private PrintWriter writer = null;

    @Override
    public void init() {

        try {

            writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream("downloadtime.csv")))), true);
            writer.println("height,time,dtime,bps,transations,dtransactions,tps");
            Apl.getBlockchainProcessor().addListener(new Listener<Block>() {

                final int interval = 10000;
                final long startTime = System.currentTimeMillis();
                long previousTime = 0;
                long transactions = 0;
                long dtransactions = 0;

                @Override
                public void notify(Block block) {
                    int n = block.getTransactions().size();
                    transactions += n;
                    dtransactions += n;
                    int height = block.getHeight();
                    if (height % interval == 0) {
                        long time = System.currentTimeMillis() - startTime;
                        writer.print(height);
                        writer.print(',');
                        writer.print(time/1000);
                        writer.print(',');
                        long dtime = (time - previousTime)/1000;
                        writer.print(dtime);
                        writer.print(',');
                        writer.print(interval/dtime);
                        writer.print(',');
                        writer.print(transactions);
                        writer.print(',');
                        writer.print(dtransactions);
                        writer.print(',');
                        long tps = dtransactions/dtime;
                        writer.println(tps);
                        previousTime = time;
                        dtransactions = 0;
                    }
                }

            }, BlockchainProcessor.Event.BLOCK_PUSHED);

        } catch (IOException e) {
            Logger.logErrorMessage(e.getMessage(), e);
        }

    }

    @Override
    public void shutdown() {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    @Override
    public void processRequest(Map<String, String> params) {
        Logger.logInfoMessage(params.get("downloadTimerMessage"));
    }
}
