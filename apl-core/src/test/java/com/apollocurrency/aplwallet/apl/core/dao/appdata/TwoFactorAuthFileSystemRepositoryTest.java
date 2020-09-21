/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.core.db.AbstractTwoFactorAuthRepositoryTest;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

@Slf4j
@Testcontainers
public class TwoFactorAuthFileSystemRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {
    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.5")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private TwoFactorAuthRepository repository;
    private Path repositoryPath;

    public static void deleteDir(Path dir, Predicate<Path> deleteFilter) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (deleteFilter.test(file)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (deleteFilter.test(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @BeforeEach
    public void setUp() throws IOException {
        repositoryPath = Files.createTempDirectory("test2faFSrepository");
        repository = new TwoFactorAuthFileSystemRepository(repositoryPath);
        setRepository(repository);
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        String confirmedAccount = Convert2.defaultRsAccount(td.ENTITY1.getAccount());
        String unconfirmedAccount = Convert2.defaultRsAccount(td.ENTITY2.getAccount());
        JSON.writeJson(repositoryPath.resolve(confirmedAccount), td.ENTITY1);
        JSON.writeJson(repositoryPath.resolve(unconfirmedAccount), td.ENTITY2);
    }

    @AfterEach
    public void tearDown() throws IOException {
        deleteDir(repositoryPath, (path) -> true);
    }
}
