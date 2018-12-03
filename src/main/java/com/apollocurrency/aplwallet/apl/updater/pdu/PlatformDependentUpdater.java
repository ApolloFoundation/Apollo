package com.apollocurrency.aplwallet.apl.updater.pdu;

import java.nio.file.Path;

public interface PlatformDependentUpdater {
    void start(Path updateDirectory);
}
