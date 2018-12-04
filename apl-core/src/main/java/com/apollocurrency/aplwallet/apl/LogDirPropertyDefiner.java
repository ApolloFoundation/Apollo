/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.io.File;
import java.nio.file.Paths;

import ch.qos.logback.core.PropertyDefinerBase;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
    private String logDirectory;

    @Override
    public String getPropertyValue() {
        File logDir = Apl.getLogDir();
        if (logDir != null) {
            return logDir.toPath().resolve(logDirectory).toString();
        }
        return Paths.get("").resolve("logs").resolve(logDirectory).toAbsolutePath().toString();
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }
}

