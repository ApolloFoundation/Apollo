/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;
import com.apollocurrency.aplwallet.apl.updater.UpdaterMediator;

public class LinuxPlatformDependentUpdater extends UnixPlatformDependentUpdater {
    public LinuxPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(runTool, updateScriptPath, updaterMediator, updateInfo);
    }
}
