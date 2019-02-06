/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */
/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirProviderUtilTest {
    @BeforeEach
    public void setUp() {
        RuntimeEnvironment.getInstance().setMain(this.getClass());
    }
    @Test
    public void testGetBinDirectory() throws URISyntaxException {
        Path binDir = DirProvider.getBinDir();
        String userDir =
                Paths.get(this.getClass().getClassLoader().getResource("").toURI()).getParent().getParent().getParent().toAbsolutePath().toString();
        Assertions.assertEquals(userDir, binDir.toAbsolutePath().toString());
    }
}
