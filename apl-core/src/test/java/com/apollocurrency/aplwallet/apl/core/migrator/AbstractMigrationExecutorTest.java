/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractMigrationExecutorTest {
    private final String deleteProp;
    private final String migrationProp;
    private final String path;
    private final String pathProp;

    public AbstractMigrationExecutorTest(String deleteProp, String migrationProp, String path, String pathProp ) {
        this.deleteProp = deleteProp;
        this.migrationProp = migrationProp;
        this.path = path;
        this.pathProp = pathProp;
    }

    private static PropertiesHolder propertiesHolder  = new PropertiesHolder();
    private static Properties properties = new Properties();
    private DatabaseManager databaseManager;
    @Rule
    private TemporaryFolder folder = new TemporaryFolder();

    @BeforeEach
    public void setUp() throws IOException {
        folder.create();
        databaseManager = new DatabaseManager(DbTestData.DB_MEM_PROPS, propertiesHolder);
    }

    @AfterEach
    public void tearDown() {
        databaseManager.shutdown();
    }

    public abstract MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder);

    @Test
    public void testMigrationDirectories() {
        initProperties(true);

        MigrationExecutor executor = getExecutor(databaseManager, propertiesHolder);
        List<Path> srcPaths = executor.getSrcPaths();
        Assertions.assertNotNull(srcPaths);
        Assertions.assertEquals(1, srcPaths.size());
        Path actual = srcPaths.get(0);
        Assertions.assertFalse(actual.toAbsolutePath().toString().contains(File.separator + "." + File.separator));
        Assertions.assertTrue(actual.startsWith(System.getProperty("user.home")));
        Assertions.assertTrue(actual.endsWith(Paths.get(path).normalize()));
    }

    private void initProperties(boolean delete) {
        properties.put(pathProp, path);
        properties.put(deleteProp, Boolean.toString(delete));
        propertiesHolder.init(properties);
    }

    @Test
    public void testPerformMigration() throws IOException {

        initProperties(true);

        File srcFolder = folder.newFolder();
        Files.createFile(srcFolder.toPath().resolve("1"));
        Files.createFile(srcFolder.toPath().resolve("2"));

        File destDir = folder.newFolder();
        MigrationExecutor executor = Mockito.spy(getExecutor(databaseManager,
                propertiesHolder));
        Mockito.doReturn(Arrays.asList(srcFolder.toPath())).when(executor).getSrcPaths();
        executor.performMigration(destDir.toPath());
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        Assertions.assertFalse(Boolean.parseBoolean(optionDAO.get(migrationProp)));
        Assertions.assertEquals(2, Files.list(destDir.toPath()).count());
        List<Path> paths = Files.list(folder.getRoot().toPath()).collect(Collectors.toList());
        Assertions.assertEquals(1, paths.size());
        Assertions.assertEquals(destDir.toPath(), paths.get(0));
    }
    @Test
    public void testPerformMigrationWithoutDeletion() throws IOException {
        initProperties(false);

        File srcFolder = folder.newFolder();
        Files.createFile(srcFolder.toPath().resolve("1"));
        Files.createFile(srcFolder.toPath().resolve("2"));

        File destDir = folder.newFolder();
        MigrationExecutor executor = Mockito.spy(getExecutor(databaseManager,
                propertiesHolder));
        Mockito.doReturn(Arrays.asList(srcFolder.toPath())).when(executor).getSrcPaths();
        executor.performMigration(destDir.toPath());
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        Assertions.assertFalse(Boolean.parseBoolean(optionDAO.get(migrationProp)));
        Assertions.assertEquals(2, Files.list(destDir.toPath()).count());
        Assertions.assertEquals(2, Files.list(srcFolder.toPath()).count());
        Stream<Path> paths = Files.list(folder.getRoot().toPath());
        Set<Path> expected = new HashSet<>();
        expected.add(destDir.toPath());
        expected.add(srcFolder.toPath());
        Assertions.assertEquals(expected, paths.collect(Collectors.toSet()));
    }
    @Test
    public void testMigrateNothing() throws IOException {
        initProperties(false);

        File srcFolder = folder.newFolder();
        File destDir = folder.newFolder();
        MigrationExecutor executor = Mockito.spy(getExecutor(databaseManager,
                propertiesHolder));
        Mockito.doReturn(Arrays.asList(srcFolder.toPath())).when(executor).getSrcPaths();
        executor.performMigration(destDir.toPath());
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        Assertions.assertFalse(Boolean.parseBoolean(optionDAO.get(migrationProp)));
        Assertions.assertEquals(0, Files.list(destDir.toPath()).count());
        Assertions.assertEquals(0, Files.list(srcFolder.toPath()).count());
        Stream<Path> paths = Files.list(folder.getRoot().toPath());
        Set<Path> expected = new HashSet<>();
        expected.add(destDir.toPath());
        expected.add(srcFolder.toPath());
        Assertions.assertEquals(expected, paths.collect(Collectors.toSet()));
    }
}
