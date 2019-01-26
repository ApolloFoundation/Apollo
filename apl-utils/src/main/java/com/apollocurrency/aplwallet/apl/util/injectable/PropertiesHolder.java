/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.inject.Singleton;

import org.slf4j.Logger;

@Singleton
public class PropertiesHolder {
    
    private static final Logger LOG = getLogger(PropertiesHolder.class);

    private Properties properties;

    public PropertiesHolder() {
        LOG.error("Empty constructor called, which is must not be called!");
    }

    public void init(Properties properties){
        this.properties = properties;
    }

    public int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            LOG.info(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            LOG.info(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
    }

    public String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        return getStringProperty(name, defaultValue, doNotLog, null);
    }

    public String getStringProperty(String name, String defaultValue, boolean doNotLog, String encoding) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            LOG.info(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
        } else {
            LOG.info(name + " not defined");
            value = defaultValue;
        }
        if (encoding == null || value == null) {
            return value;
        }
        try {
            return new String(value.getBytes("ISO-8859-1"), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getStringListProperty(String name) {
        return getStringListProperty(name, Collections.emptyList());
    }
    public List<String> getStringListProperty(String name, List<String> defaultValue) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result.isEmpty() ? defaultValue : result;
    }

    public boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            LOG.info(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            LOG.info(name + " = \"false\"");
            return false;
        }
        LOG.info(name + " not defined, using default " + defaultValue);
        return defaultValue;
    }
}
