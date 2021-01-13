package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.util.env.OS;

public interface PlatformDependentUpdaterFactory {
    PlatformDependentUpdater createInstance(OS OS, UpdateInfo updateInfo);
}
