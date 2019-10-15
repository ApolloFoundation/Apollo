/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private TemporaryFolderExtension folder;
    public AbstractMigrationExecutorTest(String deleteProp, String migrationProp, String path, String pathProp ) {
        this.deleteProp = deleteProp;
        this.migrationProp = migrationProp;
        this.path = path;
        this.pathProp = pathProp;
    }

    private static PropertiesHolder propertiesHolder  = new PropertiesHolder();
    private static Properties properties = new Properties();
    private DatabaseManager databaseManager;


    @BeforeEach
    public void setUp() throws IOException {
        databaseManager = new DatabaseManagerImpl(DbTestData.getInMemDbProps(), propertiesHolder, new JdbiHandleFactory());
        folder = getTempFolder();
    }

    @AfterEach
    public void tearDown() {
        databaseManager.shutdown();
    }

    public abstract MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder);

    public abstract TemporaryFolderExtension getTempFolder();
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
        List<Path> paths = Files.list(folder.getRoot().toPath()).filter(Files::exists).collect(Collectors.toList());
        Assertions.assertEquals(1, paths.size());
        Assertions.assertEquals(destDir.toPath(), paths.get(0));
    }

    @Test
    public void testPerformMigrationToSelfWithDeletion() throws IOException {

        initProperties(true);

        File srcFolder = folder.newFolder();
        Files.createFile(srcFolder.toPath().resolve("1"));
        Files.createFile(srcFolder.toPath().resolve("2"));

        File destDir = srcFolder;
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
    public void testPerformMigrationToInnerFolderWithDeletion() throws IOException {

        initProperties(true);

        File srcFolder = folder.newFolder();
        Files.createFile(srcFolder.toPath().resolve("1"));
        Files.createFile(srcFolder.toPath().resolve("2"));
        File destDir = srcFolder.toPath().resolve("dest").toFile();
        MigrationExecutor executor = Mockito.spy(getExecutor(databaseManager,
                propertiesHolder));
        Mockito.doReturn(Arrays.asList(srcFolder.toPath())).when(executor).getSrcPaths();
        executor.performMigration(destDir.toPath());
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        Assertions.assertFalse(Boolean.parseBoolean(optionDAO.get(migrationProp)));
        Assertions.assertEquals(2, Files.list(destDir.toPath()).count());
        List<Path> paths = Files.list(srcFolder.toPath()).collect(Collectors.toList());
        Assertions.assertEquals(paths.size(), 1);
        paths = Files.list(folder.getRoot().toPath()).collect(Collectors.toList());
        Assertions.assertEquals(paths.size(), 1);
        Assertions.assertEquals(paths.get(0), srcFolder.toPath());
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
