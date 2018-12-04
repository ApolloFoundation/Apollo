/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.io.IOException;
import java.nio.file.Path;

public interface Unpacker {
    /**
     * Unpack file
     * @param file - path to file which should be unpacked
     * @return path to unpacked file
     * @throws IOException when I/O error occurred during unpacking
     */
    Path unpack(Path file) throws IOException;
}
