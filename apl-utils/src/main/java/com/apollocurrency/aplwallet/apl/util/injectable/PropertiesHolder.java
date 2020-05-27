/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import javax.enterprise.inject.Vetoed;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Vetoed
public class PropertiesHolder {

    private Properties properties;

    public void init(Properties properties) {
        this.properties = properties;
    }

    public int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public int getIntProperty(String name, int defaultValue) {
        try {
            if (properties == null) {
                return defaultValue;
            }
            int result = Integer.parseInt(properties.getProperty(name));
            return result;
        } catch (NumberFormatException e) {
            log.trace("{} not defined or not numeric, using default value {}", name, defaultValue);
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
        if (properties == null) {
            return defaultValue;
        }
        String value = properties.getProperty(name);
        if (value == null || "".equals(value)) {
            log.trace("{} not defined", name);
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
        if (properties == null) {
            return defaultValue;
        }
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            return false;
        }
        log.trace("{} not defined, using default {}", name, defaultValue);
        return defaultValue;
    }

    public boolean isOffline() {
        return getBooleanProperty("apl.isOffline");
    }

    public boolean isLightClient() {
        return getBooleanProperty("apl.isLightClient");
    }

    public String customLoginWarning() {
        return getStringProperty("apl.customLoginWarning", null, false, "UTF-8");
    }

    public int MAX_ROLLBACK() {
        return Math.max(getIntProperty("apl.maxRollback"), 720);
    }

    public int FORGING_DELAY() {
        return getIntProperty("apl.forgingDelay");
    }

    public int FORGING_SPEEDUP() {
        return getIntProperty("apl.forgingSpeedup");
    }

    public int BATCH_COMMIT_SIZE() {
        return getIntProperty("apl.batchCommitSize", Integer.MAX_VALUE);
    }

    public int TRIM_TRANSACTION_TIME_THRESHHOLD() {
        return getIntProperty("apl.trimOperationsLogThreshold", 1000);
    }

    public boolean INCLUDE_EXPIRED_PRUNABLE() {
        return getBooleanProperty("apl.includeExpiredPrunable");
    }

    public boolean correctInvalidFees() {
        return getBooleanProperty("apl.correctInvalidFees");
    }

    @Override
    public String toString() {
        return "PropertiesHolder{ properties size=[" + (properties != null ? properties.size() : -1) + "'] }";
    }

    public String dumpAllProperties() {
        final StringBuilder sb = new StringBuilder("PropertiesHolder_DUMP : \n");
        properties.forEach((k, v) -> {
                if (!k.equals("adminPassword")) {
                    sb.append("\t'").append(k).append("' <- ").append(v).append(", ");
                } else {
                    sb.append("\t'").append(k).append("' <- ").append("***").append(", ");
                }
            }
        );
        sb.append('\n');
        return sb.toString();
    }

    public Properties getProperties() {
        return properties;
    }
}
