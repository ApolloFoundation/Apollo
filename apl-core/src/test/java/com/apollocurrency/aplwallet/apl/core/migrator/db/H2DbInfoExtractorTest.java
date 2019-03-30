/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;


import com.apollocurrency.aplwallet.apl.data.BlockTestData;

import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class H2DbInfoExtractorTest {
    public static final String DB_SUFFIX = ".h2.db";
    private H2DbInfoExtractor h2DbInfoExtractor = new H2DbInfoExtractor("sa", "sa");
    private static Path path = Paths.get(System.getProperty("java.io.tmpdir"), "dbInfoExtractor");

    @BeforeEach
    void setUp() throws IOException {
        DbManipulator manipulator = new DbManipulator(path);
        manipulator.init();
        manipulator.populate();
        manipulator.shutdown();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.delete(Paths.get(path.toString() + DB_SUFFIX));
    }

    @Test
    public void testGetHeight() {
        int height = h2DbInfoExtractor.getHeight(path.toString());
        BlockTestData btd = new BlockTestData();
        Assertions.assertEquals(btd.BLOCK_11.getHeight(), height);

    }

    @Test
    public void testGetPath() {
        String path = H2DbInfoExtractorTest.path.toAbsolutePath().toString();
        Assertions.assertEquals(Paths.get(path + DB_SUFFIX), h2DbInfoExtractor.getPath(path));
    }
}
