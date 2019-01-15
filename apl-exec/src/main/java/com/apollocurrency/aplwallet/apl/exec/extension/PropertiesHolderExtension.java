/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec.extension;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import java.lang.reflect.Field;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.exec.Apollo;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class PropertiesHolderExtension implements Extension {

    void processInjectionTarget(@Observes ProcessInjectionTarget<PropertiesHolder> pit) {

        /* wrap this to intercept the component lifecycle */

        final InjectionTarget<PropertiesHolder> it = pit.getInjectionTarget();

        InjectionTarget<PropertiesHolder> wrapped = new InjectionTarget<PropertiesHolder>() {
            @Override
            public void inject(PropertiesHolder instance, CreationalContext<PropertiesHolder> ctx) {
                it.inject(instance, ctx);
                try {
                    Field properties = PropertiesHolder.class.getDeclaredField("properties");
                    properties.setAccessible(true);
                    properties.set(instance, Apollo.getPropertiesLoader().getProperties());
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
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
