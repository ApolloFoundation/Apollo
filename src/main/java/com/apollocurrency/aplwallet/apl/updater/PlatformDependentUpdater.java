/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.UpdaterDb;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

public class PlatformDependentUpdater {
        private static final org.slf4j.Logger LOG = getLogger(PlatformDependentUpdater.class);

    private UpdaterMediator mediator = UpdaterMediator.getInstance();
    public static PlatformDependentUpdater getInstance() {
        return PlatformDpendentUpdaterHolder.INSTANCE;
    }

    public void continueUpdate(Path updateDirectory, Platform platform) {
        switch (platform) {
            case WINDOWS:
                shutdownAndRunScript(updateDirectory, WINDOWS_UPDATE_SCRIPT_PATH, WINDOWS_RUN_TOOL_PATH);
                break;
            case LINUX:
                shutdownAndRunScript(updateDirectory, LINUX_UPDATE_SCRIPT_PATH, LINUX_RUN_TOOL_PATH);
                break;
            case OSX:
                shutdownAndRunScript(updateDirectory, OSX_UPDATE_SCRIPT_PATH, OSX_RUN_TOOL_PATH);
                break;
        }
        mediator.shutdownApplication();
    }

    private void shutdownAndRunScript(Path updateDirectory, String scriptName, String runTool) {
        Thread scriptRunner = new Thread(() -> {
        LOG.debug("Waiting apl shutdown...");
            UpdaterDb.saveUpdateStatus(true);
            while (!mediator.isShutdown()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                catch (InterruptedException e) {
                    Logger.logErrorMessage("Platform dependent updater's thread was awakened", e);
                }
            }
            Path scriptPath = updateDirectory.resolve(scriptName);
            if (!Files.exists(scriptPath)) {
                LOG.error("File {} not exist in update directory! Cannot continue update.", scriptPath);
                System.exit(1);
            }
            try {
                LOG.debug("Starting platform dependent script");
                Runtime.getRuntime().exec(String.format("%s %s %s %s %s", runTool, scriptPath.toString(), Paths.get("").toAbsolutePath().toString(), updateDirectory.toAbsolutePath().toString(), RuntimeEnvironment.isDesktopApplicationEnabled()).trim());
                LOG.debug("Platform dependent script was started");
            }
            catch (IOException e) {
                LOG.error("Cannot execute update script: " + scriptPath, e);
            }
            LOG.debug("Exit...");
            System.exit(0);
        }, "Platform dependent update thread");
        scriptRunner.start();
    }

    private static class PlatformDpendentUpdaterHolder {
        private static final PlatformDependentUpdater INSTANCE = new PlatformDependentUpdater();
    }
}
