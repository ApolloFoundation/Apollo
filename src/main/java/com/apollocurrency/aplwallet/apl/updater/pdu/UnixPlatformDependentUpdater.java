/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;

import java.io.IOException;
import java.nio.file.Path;

public class UnixPlatformDependentUpdater extends DefaultPlatformDependentUpdater {

    public UnixPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(runTool, updateScriptPath, updaterMediator, updateInfo);
    }

    @Override
    Process runCommand(Path updateDirectory, Path workingDirectory, Path appDirectory, boolean isDesktop) throws IOException {
        Process process = super.runCommand(updateDirectory, workingDirectory, appDirectory, isDesktop);
        try {
            process.waitFor();
            return process;
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
