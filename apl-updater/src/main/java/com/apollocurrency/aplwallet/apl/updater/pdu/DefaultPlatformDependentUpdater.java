/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class DefaultPlatformDependentUpdater extends AbstractPlatformDependentUpdater {
    private final String runTool;
    private final String updateScriptPath;
        private static final Logger LOG = LoggerFactory.getLogger(DefaultPlatformDependentUpdater.class);

    public DefaultPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(updaterMediator, updateInfo);
        this.runTool = runTool;
        this.updateScriptPath = updateScriptPath;
    }

    @Override
    Process runCommand(Path updateDirectory, Path workingDirectory, Path appDirectory,
                       boolean userMode, boolean isShardingOn, String chain) throws IOException {
        String[] cmdArray = new String[] {
                runTool,
                updateDirectory.resolve(updateScriptPath).toAbsolutePath().toString(), //path to update script should include all subfolders
                appDirectory.toAbsolutePath().toString(),
                updateDirectory.toAbsolutePath().toString(),
                String.valueOf(userMode),
                String.valueOf(isShardingOn), // true if sharding is enabled on node
                chain
        };
        LOG.info("Runscript params {}", Arrays.toString(cmdArray));
        LOG.info("Working directory {}", workingDirectory.toFile().getPath());
        return Runtime.getRuntime().exec(cmdArray, null, DirProvider.getBinDir().toAbsolutePath().toFile());
    }
}
