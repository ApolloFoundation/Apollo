/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec;

import ch.qos.logback.core.PropertyDefinerBase;
import com.apollocurrency.aplwallet.apl.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
    private static final Path DEFAULT_LOG_PATH = Paths.get("").resolve(Constants.APPLICATION_DIR_NAME + "-logs").toAbsolutePath();
    @Override
    public String getPropertyValue() {
        Path logDirPath = DEFAULT_LOG_PATH;

        if (Apollo.logDirPath != null) {
            if (Files.exists(Apollo.logDirPath)) {
                logDirPath = Apollo.logDirPath;
            } else {
                try {
                    Files.createDirectories(Apollo.logDirPath);
                    if (Files.isWritable(Apollo.logDirPath)) {
                        logDirPath = Apollo.logDirPath;

                    }
                }
                catch (IOException e) {
                    System.out.println(e.toString());
                }
            }
        }
        return logDirPath.toAbsolutePath().toString();
    }
}

