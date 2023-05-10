/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;

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
        String defaultValue = getDefaultValue(ip);
        if (defaultValue != null) {
            return propertiesHolder.getStringProperty((getKey(ip)), defaultValue);
        } else {
            return propertiesHolder.getStringProperty(getKey(ip));
        }
    }

    private String getDefaultValue(InjectionPoint ip) {
        String defaultValue = ip.getAnnotated().getAnnotation(Property.class).defaultValue();
        return defaultValue.trim().isEmpty() ? null : defaultValue;
    }

    @Property
    @Produces
    public int produceInt(final InjectionPoint ip) {
        String defaultValue = getDefaultValue(ip);
        if (defaultValue != null) {
            return propertiesHolder.getIntProperty((getKey(ip)), Integer.parseInt(defaultValue));
        } else {
            return propertiesHolder.getIntProperty((getKey(ip)));
        }
    }

    @Property
    @Produces
    public boolean produceBoolean(final InjectionPoint ip) {
        String defaultValue = getDefaultValue(ip);
        if (defaultValue != null) {
            return propertiesHolder.getBooleanProperty((getKey(ip)), Boolean.parseBoolean(defaultValue));
        } else {
            return propertiesHolder.getBooleanProperty((getKey(ip)));
        }
    }

    @Property
    @Produces
    public List<String> produceListOfStrings(final InjectionPoint ip) {
        String defaultValue = getDefaultValue(ip);
        if (defaultValue != null) {
            return propertiesHolder.getStringListProperty((getKey(ip)), Arrays.asList(defaultValue.split(";")));
        } else {
            return propertiesHolder.getStringListProperty((getKey(ip)));
        }
    }

    private String getKey(final InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        if (annotated.isAnnotationPresent(Property.class)) {
            Property annotation = annotated.getAnnotation(Property.class);
            String propertyName = annotation.name();
            if (StringUtils.isNotBlank(propertyName)) {
                return propertyName;
            }
            String value = annotation.value();
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return ip.getMember().getName();
    }
}
