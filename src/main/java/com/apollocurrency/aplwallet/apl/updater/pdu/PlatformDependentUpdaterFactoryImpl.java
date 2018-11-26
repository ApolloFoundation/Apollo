/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;

public class PlatformDependentUpdaterFactoryImpl implements PlatformDependentUpdaterFactory {
    private UpdaterMediator updaterMediator;

    public PlatformDependentUpdaterFactoryImpl(UpdaterMediator updaterMediator) {
        this.updaterMediator = updaterMediator;
    }

    @Override
    public PlatformDependentUpdater createInstance(Platform platform, UpdateInfo updateInfo) {
        switch (platform) {
            case LINUX:
                return new LinuxPlatformDependentUpdater(UpdaterConstants.LINUX_RUN_TOOL_PATH, UpdaterConstants.LINUX_UPDATE_SCRIPT_PATH,
                        updaterMediator, updateInfo);
            case MAC_OS:
                return new MacOSPlatformDependentUpdater(UpdaterConstants.MAC_OS_RUN_TOOL_PATH, UpdaterConstants.MAC_OS_UPDATE_SCRIPT_PATH, updaterMediator, updateInfo);
            case WINDOWS:
                return new WindowsPlatformDependentUpdater(UpdaterConstants.WINDOWS_RUN_TOOL_PATH, UpdaterConstants.WINDOWS_UPDATE_SCRIPT_PATH, updaterMediator, updateInfo);
            default:
                throw new IllegalArgumentException("Platform " + platform + " is not supported!");
        }
    }
}
