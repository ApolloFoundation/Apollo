/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.util.env.OS;

public class PlatformDependentUpdaterFactoryImpl implements PlatformDependentUpdaterFactory {
    private UpdaterMediator updaterMediator;

    public PlatformDependentUpdaterFactoryImpl(UpdaterMediator updaterMediator) {
        this.updaterMediator = updaterMediator;
    }

    @Override
    public PlatformDependentUpdater createInstance(OS OS, UpdateInfo updateInfo) {
        switch (OS) {
            case LINUX:
                return new LinuxPlatformDependentUpdater(UpdaterConstants.LINUX_RUN_TOOL_PATH, UpdaterConstants.LINUX_UPDATE_SCRIPT_PATH,
                    updaterMediator, updateInfo);
            case MAC_OS:
                return new MacOSPlatformDependentUpdater(UpdaterConstants.MAC_OS_RUN_TOOL_PATH, UpdaterConstants.MAC_OS_UPDATE_SCRIPT_PATH, updaterMediator, updateInfo);
            case WINDOWS:
                return new WindowsPlatformDependentUpdater(UpdaterConstants.WINDOWS_RUN_TOOL_PATH, UpdaterConstants.WINDOWS_UPDATE_SCRIPT_PATH, updaterMediator, updateInfo);
            default:
                throw new IllegalArgumentException("Platform " + OS + " is not supported!");
        }
    }
}
