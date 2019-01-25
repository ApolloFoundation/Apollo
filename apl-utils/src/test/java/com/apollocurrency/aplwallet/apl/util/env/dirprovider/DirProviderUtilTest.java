/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */
/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DirProviderUtilTest {
    @BeforeEach
    public void setUp() {
        RuntimeEnvironment.getInstance().setMain(this.getClass());
    }
    @Test
    public void testGetBinDirectory() {
        Path binDir = DirProviderUtil.getBinDir();
        String userDir =
                Paths.get(this.getClass().getClassLoader().getResource("").getPath()).getParent().getParent().getParent().toAbsolutePath().toString();
        Assertions.assertEquals(userDir, binDir.toAbsolutePath().toString());
    }
}
