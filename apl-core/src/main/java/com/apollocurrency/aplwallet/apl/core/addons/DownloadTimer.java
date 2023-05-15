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
 * Copyright © 2018-2109 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.addons;


import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import org.slf4j.Logger;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Vetoed
public final class DownloadTimer implements AddOn {
    private static final Logger LOG = getLogger(DownloadTimer.class);
    private final int interval = 10000;
    private final long startTime = System.currentTimeMillis();
    private PrintWriter writer = null;
    private long previousTime = 0;
    private long transactions = 0;
    private long dtransactions = 0;
    private Blockchain blockchain;

    @Override
    public void init() {
        try {

            writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream("downloadtime.csv")))), true);
            writer.println("height,time,dtime,bps,transations,dtransactions,tps");
            blockchain = CDI.current().select(Blockchain.class).get();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        long n = blockchain.getBlockTransactionCount(block.getId());
        transactions += n;
        dtransactions += n;
        int height = block.getHeight();
        if (height % interval == 0) {
            long time = System.currentTimeMillis() - startTime;
            writer.print(height);
            writer.print(',');
            writer.print(time / 1000);
            writer.print(',');
            long dtime = (time - previousTime) / 1000;
            writer.print(dtime);
            writer.print(',');
            writer.print(interval / dtime);
            writer.print(',');
            writer.print(transactions);
            writer.print(',');
            writer.print(dtransactions);
            writer.print(',');
            long tps = dtransactions / dtime;
            writer.println(tps);
            previousTime = time;
            dtransactions = 0;

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
        LOG.info(params.get("downloadTimerMessage"));
    }
}
