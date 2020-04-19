/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model.HeightMonitorConfig;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.web.JettyServer;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class HeightMonitor {
    private static final Logger log = getLogger(HeightMonitor.class);
    private static final int DEFAULT_DELAY = 30;

    private ScheduledExecutorService executor;
    private int delay;
    private AplContainer container;

    public HeightMonitor(Integer delay) {
        this.executor = Executors.newScheduledThreadPool(1);
        this.delay = delay == null ? DEFAULT_DELAY : delay;
    }

    public HeightMonitor() {
        this(null);
    }

    public void start(HeightMonitorConfig config) {
        try {
            this.container = AplContainer.builder().containerId("MAIN-APL-CDI")
                .annotatedDiscoveryMode()
//                    .recursiveScanPackages(JettyServer.class)
                .devMode() // enable for dev only
                .build();
            HeightMonitorService service = CDI.current().select(HeightMonitorService.class).get();
            CDI.current().select(JettyServer.class).get();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            service.setUp(config);
            executor.scheduleWithFixedDelay(service::updateStats, 0, delay, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            executor.shutdown();
            container.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
