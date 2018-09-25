/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import ch.qos.logback.core.PropertyDefinerBase;

import java.io.File;
import java.nio.file.Paths;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
    private String logfile;

    @Override
    public String getPropertyValue() {
        File logDir = Apl.getLogDir();
        if (logDir != null) {
            return logDir.toPath().resolve(Paths.get(logfile)).toString();
        }
        return Paths.get("").resolve("logs").resolve(logfile).toAbsolutePath().toString();
    }

    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }
}

