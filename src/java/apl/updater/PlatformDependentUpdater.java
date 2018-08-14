/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import apl.UpdaterMediator;
import apl.env.RuntimeEnvironment;
import apl.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static apl.updater.UpdaterConstants.*;
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

    private void shutdownAndRunScript(Path workingDirectory, String scriptName, String runTool) {
        Thread scriptRunner = new Thread(() -> {
            LOG.debug("Waiting apl shutdown...");
            while (!mediator.isShutdown()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Logger.logErrorMessage("Platform dependent updater's thread was awakened", e);
                }
            }
            Path scriptPath = workingDirectory.resolve(scriptName);
            if (!Files.exists(scriptPath)) {
                LOG.error("File {} not exist in update directory! Cannot continue update.", scriptPath);
                System.exit(1);
            }
            try {
                LOG.debug("Starting platform dependent script");
                Runtime.getRuntime().exec(String.format("%s %s %s %s %s", runTool, scriptPath.toString(), Paths.get("").toAbsolutePath().toString(), workingDirectory.toAbsolutePath().toString(), RuntimeEnvironment.isDesktopApplicationEnabled()).trim());
                LOG.debug("Platform dependent script was started");
            } catch (IOException e) {
                LOG.error("Cannot execute update script: " + scriptPath, e);
            }
            LOG.debug("Exit...");
            System.exit(0);
        }, "Windows Platform dependent update thread");
        scriptRunner.start();
    }

    public static void main(String[] args) {
        PlatformDependentUpdater instance = getInstance();
        instance.continueUpdate(Paths.get(""), Platform.WINDOWS);
    }

    private static class PlatformDpendentUpdaterHolder {
        private static final PlatformDependentUpdater INSTANCE = new PlatformDependentUpdater();
    }
}
