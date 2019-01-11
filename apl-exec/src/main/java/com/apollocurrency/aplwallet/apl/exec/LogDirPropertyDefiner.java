/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec;

import java.io.File;
import java.nio.file.Paths;

import ch.qos.logback.core.PropertyDefinerBase;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
  
    @Override
    public String getPropertyValue() {
        if (Apollo.logDir != null) {
            File log = new File(Apollo.logDir);            
            return log.getAbsolutePath().toString();
        }
        return Paths.get("").resolve("logs").toAbsolutePath().toString();
    }
}

