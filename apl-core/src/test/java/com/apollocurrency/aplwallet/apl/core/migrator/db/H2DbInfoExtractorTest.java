/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;


class H2DbInfoExtractorTest {
/*    private static Path path = Paths.get(System.getProperty("java.io.tmpdir"), "dbInfoExtractor" + DbProperties.DB_EXTENSION_WITH_DOT);
    private H2DbInfoExtractor h2DbInfoExtractor = new H2DbInfoExtractor("sa", "sa");

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
    }*/
}
