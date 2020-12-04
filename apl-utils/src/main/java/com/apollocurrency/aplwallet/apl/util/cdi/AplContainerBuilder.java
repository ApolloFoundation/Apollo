package com.apollocurrency.aplwallet.apl.util.cdi;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.ArcContainerImpl;
/*
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for building AplContainer with Weld container instance inside.
 * @deprecated
 */
@Deprecated
public class AplContainerBuilder {
    private static final Logger log = LoggerFactory.getLogger(AplContainerBuilder.class);

    private boolean devMode = false;

    private String containerId;

    private Boolean annotatedDiscoveryMode;

    private Boolean disableDiscovery;

    private List<Class<?>> interceptors;

    private boolean concurrentDeploymentDisabled = false;

//    private List<Class<?>> recursiveScanPackages;

    public AplContainerBuilder containerId(String id) {
        containerId = id;
        return this;
    }

    public AplContainerBuilder annotatedDiscoveryMode() {
        annotatedDiscoveryMode = true;
        disableDiscovery = null;
        return this;
    }

    public AplContainerBuilder disableDiscovery() {
        annotatedDiscoveryMode = null;
        disableDiscovery = true;
        return this;
    }

    public AplContainerBuilder devMode() {
        devMode = true;
        System.setProperty("org.jboss.weld.probe.jmxSupport", "true");
        return this;
    }

    public AplContainerBuilder disableConcurrentDeployment() {
        concurrentDeploymentDisabled = true;
        return this;
    }

    public AplContainerBuilder interceptors(Class<?>... interceptors) {
        if (interceptors != null && interceptors.length > 0) {
            this.interceptors = Arrays.stream(interceptors).collect(Collectors.toList());
        }
        return this;
    }

//    public AplContainerBuilder recursiveScanPackages(Class<?>... recursiveScanPackages) {
//        if (recursiveScanPackages != null && recursiveScanPackages.length > 0) {
//            this.recursiveScanPackages = Arrays.stream(recursiveScanPackages).collect(Collectors.toList());
//        }
//        return this;
//    }

    public AplContainer build() {
        log.debug("Apollo DI container build()...");

        ArcContainer newContainer = new ArcContainerImpl();
/*
        if (containerId != null && !containerId.isEmpty()) {
            newContainer.containerId(containerId);
        }

        if (annotatedDiscoveryMode != null && annotatedDiscoveryMode) {
            newContainer.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
        }

        if (disableDiscovery != null && disableDiscovery) {
            newContainer.disableDiscovery();
        }

        if (interceptors != null && !interceptors.isEmpty()) {
            interceptors.forEach(newContainer::addInterceptor);
        }

        if (devMode) {
            newContainer.enableDevMode();
        }

        if (concurrentDeploymentDisabled) {
            newContainer.property(ConfigurationKey.CONCURRENT_DEPLOYMENT.get(), "false");
        }
*/
        newContainer.beanManager();
        ((ArcContainerImpl)newContainer).init();

        if (newContainer.isRunning()) {
            log.error("Weld container is unsatisfied!");
        }
        log.debug("Apollo DI container - {}", !newContainer.isRunning() ? "DONE" : "ERROR");

        return new AplContainer(newContainer);
    }

}
