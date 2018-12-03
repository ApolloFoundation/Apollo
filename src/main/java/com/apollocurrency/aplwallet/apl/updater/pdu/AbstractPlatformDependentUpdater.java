/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.MAX_SHUTDOWN_TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractPlatformDependentUpdater implements PlatformDependentUpdater {
    private static final Logger LOG = getLogger(AbstractPlatformDependentUpdater.class);
    private UpdaterMediator updaterMediator;
    private UpdateInfo updateInfo;
    private int maxShutdownTimeOut = MAX_SHUTDOWN_TIMEOUT;

    public AbstractPlatformDependentUpdater(UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        this.updaterMediator = updaterMediator;
        this.updateInfo = updateInfo;
    }

    public int getMaxShutdownTimeOut() {
        return maxShutdownTimeOut;
    }

    public void setMaxShutdownTimeOut(int maxShutdownTimeOut) {
        this.maxShutdownTimeOut = maxShutdownTimeOut;
    }

    @Override
    public void start(Path updateDirectory) {
        shutdownAndRunScript(updateDirectory);
        new Thread(() -> {
            try {
                LOG.debug("Waiting before shutdown: max {} sec", maxShutdownTimeOut);
                int secondsRemaining = maxShutdownTimeOut;
                while (updateInfo.getUpdateState() != UpdateInfo.UpdateState.FINISHED && secondsRemaining-- > 0) {
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            catch (InterruptedException e) {
                LOG.error(e.toString(), e);
            }
            updaterMediator.shutdownApplication();

        }, "Updater Apollo shutdown thread").start();
    }

    abstract Process runCommand(Path updateDirectory, Path workingDirectory, Path appDirectory, boolean isDesktop) throws IOException;

    private void shutdownAndRunScript(Path updateDirectory) {
        Thread scriptRunner = new Thread(() -> {
            LOG.debug("Waiting apl shutdown...");
            while (!updaterMediator.isShutdown()) {
                try {
                    LOG.trace("WAITING...");
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                catch (InterruptedException e) {
                    LOG.debug(e.toString(), e);
                }
            }
            LOG.debug("Apl was shutdown");
            runScript(updateDirectory);
        }, "Platform dependent update thread");
        scriptRunner.start();
    }

    private void runScript(Path updateDir) {
        if (!Files.exists(updateDir)) {
            LOG.error("Update directory {} not exist ! Cannot continue update.", updateDir);
            System.exit(20);
        }
        try {
            LOG.debug("Starting platform dependent script");
            runCommand(updateDir, Paths.get("").toAbsolutePath(), Paths.get("").toAbsolutePath(), RuntimeEnvironment.isDesktopApplicationEnabled());
            LOG.debug("Platform dependent script was started");
        }
        catch (IOException e) {
            LOG.error("Cannot execute update script: ", e);
            System.exit(10);
        }
        LOG.debug("Exit...");
        System.exit(0);
    }
}
