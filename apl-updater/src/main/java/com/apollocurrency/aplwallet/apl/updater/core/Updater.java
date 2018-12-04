/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.updater.UpdateInfo;

public interface Updater {

    UpdateInfo.UpdateState processUpdate();

    Level getLevel();

    UpdateInfo getUpdateInfo();
}
