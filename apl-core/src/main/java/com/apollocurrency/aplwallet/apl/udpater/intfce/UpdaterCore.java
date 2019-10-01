/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

public interface UpdaterCore {

    void init(String updateAttachmentFile, boolean debug);

    void init();

    boolean startAvailableUpdate();

    void startUpdate(UpdateData updateData);

    UpdateInfo getUpdateInfo();

}
