/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import java.nio.file.Path;

public interface DbInfoExtractor {

    int getHeight(String dbDir);

    Path getPath(String dbDir);
}
