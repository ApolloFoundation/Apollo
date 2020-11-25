package com.apollocurrency.aplwallet.apl.util.cdi;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.ArcContainerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apollo CDI with Weld container instance inside.
 */
public class AplContainer {

    private static final Logger log = LoggerFactory.getLogger(AplContainer.class);

    private final ArcContainer container;

    public AplContainer(ArcContainer container) {
        this.container = container;
    }

    public static AplContainerBuilder builder() {
        return new AplContainerBuilder();
    }

    public void shutdown() {
        log.debug("Apollo DI container shutdown()...");
        if (container.isRunning()) {
            ((ArcContainerImpl)container).shutdown();
        }
    }

    public ArcContainer getContainer() {
        return container;
    }
}
