/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;

public class MacOSPlatformDependentUpdater extends UnixPlatformDependentUpdater {
    public MacOSPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(runTool, updateScriptPath, updaterMediator, updateInfo);
    }
}
