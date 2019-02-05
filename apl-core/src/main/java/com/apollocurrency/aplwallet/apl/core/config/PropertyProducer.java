/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.util.Arrays;
import java.util.List;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

public class PropertyProducer {
    private PropertiesHolder propertiesHolder;

    @Inject
    public PropertyProducer(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public void setPropertiesHolder(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }
    @Property
    @Produces
    public String produceString(final InjectionPoint ip) {
        return propertiesHolder.getStringProperty((getKey(ip)), getDefaultValue(ip));
    }

    private String getDefaultValue(InjectionPoint ip) {
        return ip.getAnnotated().getAnnotation(Property.class).defaultValue();
    }

    @Property
    @Produces
    public int produceInt(final InjectionPoint ip) {
        return propertiesHolder.getIntProperty((getKey(ip)), Integer.parseInt(getDefaultValue(ip)));
    }
    @Property
    @Produces
    public boolean produceBoolean(final InjectionPoint ip) {
        return propertiesHolder.getBooleanProperty((getKey(ip)), Boolean.parseBoolean(getDefaultValue(ip)));
    }
    @Property
    @Produces
    public List<String> produceListOfStrings(final InjectionPoint ip) {
        return propertiesHolder.getStringListProperty((getKey(ip)), Arrays.asList(getDefaultValue(ip).split(";")));
    }

    private String getKey(final InjectionPoint ip) {
        return (ip.getAnnotated().isAnnotationPresent(Property.class)
                && !ip.getAnnotated().getAnnotation(Property.class).name().isEmpty())
                ? ip.getAnnotated().getAnnotation(Property.class).name()
                : ip.getMember().getName();
    }
}
