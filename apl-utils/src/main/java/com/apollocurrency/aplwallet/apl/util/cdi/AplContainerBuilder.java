package com.apollocurrency.aplwallet.apl.util.cdi;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for building AplContainer with Weld container instance inside.
 */
public class AplContainerBuilder {
    private static final Logger log = LoggerFactory.getLogger(AplContainerBuilder.class);
    
    private boolean devMode = false;
    
    private String containerId;

    private Boolean annotatedDiscoveryMode;

    private Boolean disableDiscovery;

    private List<Class<?>> interceptors;

    private List<Class<?>> recursiveScanPackages;

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
    
    public AplContainerBuilder devMode(){
        devMode=true;
        return this;
    }
        
    public AplContainerBuilder interceptors(Class<?>... interceptors) {
        if (interceptors != null && interceptors.length > 0) {
            this.interceptors = Arrays.stream(interceptors).collect(Collectors.toList());
        }
        return this;
    }

    public AplContainerBuilder recursiveScanPackages(Class<?>... recursiveScanPackages) {
        if (recursiveScanPackages != null && recursiveScanPackages.length > 0) {
            this.recursiveScanPackages = Arrays.stream(recursiveScanPackages).collect(Collectors.toList());
        }
        return this;
    }

    public AplContainer build() {
        log.debug("Apollo DI container build()...");

        Weld weld = new Weld();
        if (containerId != null && !containerId.isEmpty()) {
            weld.containerId(containerId);
        }

        if (annotatedDiscoveryMode != null && annotatedDiscoveryMode) {
            weld.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
        }

        if (disableDiscovery != null && disableDiscovery) {
            weld.disableDiscovery();
        }

        if (interceptors != null && !interceptors.isEmpty()) {
            interceptors.forEach(weld::addInterceptor);
        }

        if (recursiveScanPackages != null && !recursiveScanPackages.isEmpty()) {
            recursiveScanPackages.forEach(p -> weld.addPackage(true, p));
        }

        if(devMode){
          weld.enableDevMode();
        }
        
        WeldContainer newContainer = weld.initialize();

        if (newContainer.isUnsatisfied()) {
            log.error("Weld container is unsatisfied!");
        }
        log.debug("Apollo DI container - {}", !newContainer.isUnsatisfied() ? "DONE" : "ERROR");

        return new AplContainer(newContainer);
    }

}
