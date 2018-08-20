/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import ch.qos.logback.core.PropertyDefinerBase;

import java.nio.file.Paths;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
    private String logfile;

    @Override
    public String getPropertyValue() {
        return Apl.getLogDir().toPath().resolve(Paths.get(logfile)).toString();
    }

    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }
}

