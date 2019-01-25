/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec.extension;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import java.util.Objects;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.exec.Apollo;
import com.apollocurrency.aplwallet.apl.util.env.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesHolderExtension implements Extension {
    private static final Logger log = LoggerFactory.getLogger(PropertiesHolderExtension.class);

    void processInjectionTarget(@Observes ProcessInjectionTarget<PropertiesHolder> pit) {

        /* wrap this to intercept the component lifecycle */

        final InjectionTarget<PropertiesHolder> it = pit.getInjectionTarget();

        InjectionTarget<PropertiesHolder> wrapped = new InjectionTarget<PropertiesHolder>() {
            @Override
            public void inject(PropertiesHolder instance, CreationalContext<PropertiesHolder> ctx) {
                it.inject(instance, ctx);

                PropertiesLoader propertiesLoader = Apollo.getPropertiesLoader();

                Objects.requireNonNull(propertiesLoader, "Properties loader not initialized yet");
                Objects.requireNonNull(propertiesLoader.getProperties(), "Injectable properties should not be null");
                if (propertiesLoader.getProperties().isEmpty()) {
                    log.warn("Injectable properties are empty! ");
                }
                instance.init(propertiesLoader.getProperties());
            }

            @Override
            public void postConstruct(PropertiesHolder instance) {
                it.postConstruct(instance);
            }

            @Override
            public void preDestroy(PropertiesHolder instance) {
                it.dispose(instance);
            }

            @Override
            public void dispose(PropertiesHolder instance) {
                it.dispose(instance);
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return it.getInjectionPoints();
            }

            @Override
            public PropertiesHolder produce(CreationalContext<PropertiesHolder> ctx) {
                return it.produce(ctx);
            }
        };

        pit.setInjectionTarget(wrapped);
    }
}
