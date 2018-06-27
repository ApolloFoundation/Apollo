package apl.updater;

import apl.Apl;
import apl.UpdaterMediator;
import apl.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static apl.updater.UpdaterConstants.WINDOWS_UPDATE_SCRIPT_PATH;
import static org.slf4j.LoggerFactory.getLogger;

public class PlatformDependentUpdater {
        private static final org.slf4j.Logger LOG = getLogger(PlatformDependentUpdater.class);

    private UpdaterMediator mediator = UpdaterMediator.getInstance();
    public static PlatformDependentUpdater getInstance() {
        return PlatformDpendentUpdaterHolder.HOLDER_INSTANCE;
    }


    public void continueUpdate(Path updateDirectory, Platform platform) {
        switch (platform) {
            case WINDOWS: {
                LOG.info("Waiting apl shutdown...");
                Thread scriptRunner = new Thread(() -> {
                    while (mediator.isShutdown()) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                        catch (InterruptedException e) {
                            Logger.logErrorMessage("Platform dependent updater's thread was awakened", e);
                        }
                    }
                    Path scriptPath = updateDirectory.resolve(WINDOWS_UPDATE_SCRIPT_PATH);
                    try {
                        LOG.info("Starting platform dependent script");

                        Runtime.getRuntime().exec("wscript.exe " + scriptPath.toString());
                        LOG.info("Platform dependent script was started");
                    }
                    catch (IOException e) {
                        LOG.info("Cannot execute update script: " + scriptPath, e);
                    }
                    LOG.info("Exit...");
                    System.exit(0);
                }, "Windows Platform dependent update thread");
                scriptRunner.start();
                Apl.shutdown();
            }
                break;
            case LINUX:
                break;
            case OSX:
                break;
        }
    }

    public static void main(String[] args) {
        PlatformDependentUpdater instance = getInstance();
        instance.continueUpdate(Paths.get(""), Platform.WINDOWS);
    }

    private static class PlatformDpendentUpdaterHolder {
        private static final PlatformDependentUpdater HOLDER_INSTANCE = new PlatformDependentUpdater();
    }
}
