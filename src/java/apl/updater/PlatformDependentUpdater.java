package apl.updater;

import java.nio.file.Path;

public class PlatformDependentUpdater {

    private static class PlatformDpendentUpdaterHolder {
        private static final PlatformDependentUpdater HOLDER_INSTANCE = new PlatformDependentUpdater();
    }

    public static PlatformDependentUpdater getInstance() {
        return PlatformDpendentUpdaterHolder.HOLDER_INSTANCE;
    }

    public void continueUpdate(Path updateDirectory) {

    }
}
