/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.nio.file.Path;

public interface ConsistencyVerifier {
    boolean verify(Path file, byte[] hash);
}
