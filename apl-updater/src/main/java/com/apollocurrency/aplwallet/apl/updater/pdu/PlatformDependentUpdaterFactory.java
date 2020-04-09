package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.util.env.Platform;

public interface PlatformDependentUpdaterFactory {
    PlatformDependentUpdater createInstance(Platform platform, UpdateInfo updateInfo);
}
