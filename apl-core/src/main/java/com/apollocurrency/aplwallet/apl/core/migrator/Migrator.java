/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Migrator {

    List<Path> migrate(List<Path> fromPaths, Path toPath) throws IOException;
}
