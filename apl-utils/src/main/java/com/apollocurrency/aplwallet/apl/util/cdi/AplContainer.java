package com.apollocurrency.aplwallet.apl.util.cdi;

import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apollo CDI with Weld container instance inside.
 */
public class AplContainer {

    private static final Logger log = LoggerFactory.getLogger(AplContainer.class);

    private final WeldContainer container;

    public AplContainer(WeldContainer container) {
        this.container = container;
    }

    public static AplContainerBuilder builder() {
        return new AplContainerBuilder();
    }

    public void shutdown() {
        log.debug("Apollo DI container shutdown()...");
        if (container.isRunning()) {
            container.shutdown();
        }
    }

    public WeldContainer getContainer() {
        return container;
    }
}
