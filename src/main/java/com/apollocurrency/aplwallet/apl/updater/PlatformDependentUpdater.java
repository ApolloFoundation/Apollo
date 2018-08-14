/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterDb;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.env.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
        new Thread(() -> {
            try {
                LOG.debug("Waiting before shutdown: max {} sec", MAX_SHUTDOWN_TIMEOUT);
                int secondsRemaining = MAX_SHUTDOWN_TIMEOUT;
                while (UpdateInfo.getInstance().getUpdateState() != UpdateInfo.UpdateState.FINISHED && secondsRemaining-- > 0) {
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            catch (InterruptedException e) {
                LOG.error(e.toString(), e);
            }
            mediator.shutdownApplication();

        }, "Updater Apollo shutdown thread").start();
    }

    public static void main(String[] args) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), Paths.get("conf/").toAbsolutePath().toFile().toURL());
        System.setProperty("apl.runtime.mode", "desktop");
        Apl.main(null);
        getInstance().continueUpdate(Paths.get(""), Platform.LINUX);
    }

    private void shutdownAndRunScript(Path updateDirectory, String scriptName, String runTool) {
        Thread scriptRunner = new Thread(() -> {
        LOG.debug("Waiting apl shutdown...");
            UpdaterDb.saveUpdateStatus(true);
            while (!mediator.isShutdown()) {
                try {
                    LOG.debug("WAITING...");
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                catch (InterruptedException e) {
                    LOG.debug(e.toString(), e);
                }
            }
            LOG.debug("Apl was shutdown");
            Path scriptPath = updateDirectory.resolve(scriptName);
            if (!Files.exists(scriptPath)) {
                LOG.error("File {} not exist in update directory! Cannot continue update.", scriptPath);
                System.exit(20);
            }
            try {
                LOG.debug("Starting platform dependent script");
                Runtime.getRuntime().exec(String.format("%s %s %s %s %s", runTool, scriptPath.toString(), Paths.get("").toAbsolutePath().toString(), updateDirectory.toAbsolutePath().toString(), RuntimeEnvironment.isDesktopApplicationEnabled()).trim());
                LOG.debug("Platform dependent script was started");
            }
            catch (IOException e) {
                LOG.error("Cannot execute update script: " + scriptPath, e);
                System.exit(10);
            }
            LOG.debug("Exit...");
            System.exit(5);
        }, "Platform dependent update thread");
        scriptRunner.start();
    }

    private static class PlatformDpendentUpdaterHolder {
        private static final PlatformDependentUpdater INSTANCE = new PlatformDependentUpdater();
    }
}
