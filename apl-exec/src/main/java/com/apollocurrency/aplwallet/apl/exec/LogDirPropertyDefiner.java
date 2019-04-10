/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec;

import ch.qos.logback.core.PropertyDefinerBase;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class LogDirPropertyDefiner extends PropertyDefinerBase {
    private static final Path DEFAULT_LOG_PATH = DirProvider.getBinDir().resolve(Constants.APPLICATION_DIR_NAME + "-logs").toAbsolutePath();

    @Override
    public String getPropertyValue() {
        List<Path> logDirPaths = Arrays.asList(Apollo.logDirPath, DEFAULT_LOG_PATH, Paths.get(System.getProperty("java.io.tmpdir")).resolve(Constants.APPLICATION_DIR_NAME + "-logs"));

        Path logDirPath = null;
        for (int i = 0; i < logDirPaths.size(); i++) {
            Path p = logDirPaths.get(i);
            if (p != null) {
                try {
                    if (!Files.exists(p)) {
                        Files.createDirectories(p);
                    }
                    if (Files.isWritable(p)) {
                        logDirPath = p;
                        break;
                    }
                }
                catch (IOException e) {
                    System.err.println(e.toString());
                }
            }
        }
        if (logDirPath == null) {
            logDirPath = Paths.get("");
        }

        return logDirPath.toAbsolutePath().toString();
    }
}

