/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class H2DbInfoExtractorTest {
    /*
    private H2DbInfoExtractor h2DbInfoExtractor = new H2DbInfoExtractor("sa", "sa");
    private static Path path = Paths.get(System.getProperty("java.io.tmpdir"), "dbInfoExtractor" + DbProperties.DB_EXTENSION_WITH_DOT);

    @BeforeEach
    void setUp() throws IOException {
        DbManipulator manipulator = new DbManipulator(DbTestData.getDbFileProperties(path.toAbsolutePath().toString()));
        manipulator.init();
        manipulator.populate();
        manipulator.shutdown();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.delete(Paths.get(path.toString() + DbProperties.DB_EXTENSION_WITH_DOT));
    }

    @Test
    void testGetHeight() {
        int height = h2DbInfoExtractor.getHeight(path.toString());
        Assertions.assertEquals(BlockTestData.BLOCK_13_HEIGHT, height);

    }

    @Test
    public void testGetPath() {
        String path = H2DbInfoExtractorTest.path.toAbsolutePath().toString();
        Assertions.assertEquals(Paths.get(path + DbProperties.DB_EXTENSION_WITH_DOT), h2DbInfoExtractor.getPath(path));
    }
    */
}
