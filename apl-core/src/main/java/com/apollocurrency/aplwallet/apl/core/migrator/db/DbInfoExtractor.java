/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import java.nio.file.Path;

public interface DbInfoExtractor {

    int getHeight(String dbPath);

    Path getPath(String dbPath);
}
