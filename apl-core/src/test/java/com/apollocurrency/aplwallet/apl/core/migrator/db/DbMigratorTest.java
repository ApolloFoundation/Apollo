/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DbMigratorTest {
/*
    @Mock
    DbInfoExtractor dbInfoExtractor;

    private Path firstDbPath;
    private Path secondDbPath;
    private Path targetDbPath;
    private byte[] firstDbPayload = "TestPayload-1".getBytes();
    private byte[] secondDbPayload = "TestPayload-2".getBytes();

    @BeforeEach
    public void init() throws IOException {
        firstDbPath = Files.createTempFile("migrationDb-1", "h2");
        Files.write(firstDbPath, firstDbPayload);
        secondDbPath = Files.createTempFile("migrationDb-2", "h2");
        Files.write(secondDbPath, secondDbPayload);
        targetDbPath = Files.createTempFile("targetdb", "h2");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.delete(firstDbPath);
        Files.delete(secondDbPath);
        Files.delete(targetDbPath);
    }


    @Test
    public void testNothingToMigrate() throws IOException {
        DbMigrator dbMigrator = new DbMigrator(dbInfoExtractor);
        List<Path> migrated = dbMigrator.migrate(new ArrayList<>(), Paths.get(""));
        Assertions.assertEquals(0, migrated.size());
    }

    @Test
    public void testMigrateFirstDbInTheList() throws IOException {
        DbMigrator dbMigrator = new DbMigrator(dbInfoExtractor);

        doReturn(10).when(dbInfoExtractor).getHeight(firstDbPath.toAbsolutePath().toString());
        doReturn(10).when(dbInfoExtractor).getHeight(targetDbPath.toAbsolutePath().toString());
        doReturn(firstDbPath).when(dbInfoExtractor).getPath(firstDbPath.toAbsolutePath().toString());
        doReturn(targetDbPath).when(dbInfoExtractor).getPath(targetDbPath.toAbsolutePath().toString());

        List<Path> migrated = dbMigrator.migrate(Collections.singletonList(firstDbPath), targetDbPath);

        Assertions.assertEquals(1, migrated.size());
        Assertions.assertEquals(firstDbPath.getParent(), migrated.get(0));
        Assertions.assertArrayEquals(firstDbPayload, Files.readAllBytes(targetDbPath));
    }

    @Test
    public void testIncorrectMigration() throws IOException {
        DbMigrator dbMigrator = new DbMigrator(dbInfoExtractor);

        doReturn(10).when(dbInfoExtractor).getHeight(firstDbPath.toAbsolutePath().toString());
        doReturn(9).when(dbInfoExtractor).getHeight(targetDbPath.toAbsolutePath().toString());
        doReturn(firstDbPath).when(dbInfoExtractor).getPath(firstDbPath.toAbsolutePath().toString());
        doReturn(targetDbPath).when(dbInfoExtractor).getPath(targetDbPath.toAbsolutePath().toString());

        Assertions.assertThrows(RuntimeException.class, () -> dbMigrator.migrate(Collections.singletonList(firstDbPath), targetDbPath));
    }

    @Test
    public void testMigrateSecondDbInTheList() throws IOException {
        DbMigrator dbMigrator = new DbMigrator(dbInfoExtractor);

        doReturn(0).when(dbInfoExtractor).getHeight(firstDbPath.toAbsolutePath().toString());
        doReturn(10).when(dbInfoExtractor).getHeight(secondDbPath.toAbsolutePath().toString());
        doReturn(10).when(dbInfoExtractor).getHeight(targetDbPath.toAbsolutePath().toString());
        doReturn(secondDbPath).when(dbInfoExtractor).getPath(secondDbPath.toAbsolutePath().toString());
        doReturn(targetDbPath).when(dbInfoExtractor).getPath(targetDbPath.toAbsolutePath().toString());

        List<Path> migrated = dbMigrator.migrate(Arrays.asList(firstDbPath, secondDbPath), targetDbPath);

        Assertions.assertEquals(1, migrated.size());
        Assertions.assertEquals(secondDbPath.getParent(), migrated.get(0));
        Assertions.assertArrayEquals(secondDbPayload, Files.readAllBytes(targetDbPath));
    }

*/

}
