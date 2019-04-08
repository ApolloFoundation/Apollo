/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TwoFactorAuthFileSystemRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {

    private TwoFactorAuthRepository repository;
    private  Path repositoryPath;

    @BeforeEach
    public void setUp() throws IOException {
        repositoryPath = Files.createTempDirectory("test2faFSrepository");
        repository = new TwoFactorAuthFileSystemRepository(repositoryPath);
        setRepository(repository);
        TwoFactorAuthTestData td = new TwoFactorAuthTestData();
        String confirmedAccount = Convert.defaultRsAccount(td.ENTITY1.getAccount());
        String unconfirmedAccount = Convert.defaultRsAccount(td.ENTITY2.getAccount());
        JSON.writeJson(repositoryPath.resolve(confirmedAccount), td.ENTITY1);
        JSON.writeJson(repositoryPath.resolve(unconfirmedAccount), td.ENTITY2);
    }
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
    
    @AfterEach
    public  void tearDown() throws IOException {
     deleteDir(repositoryPath, (path)-> true);
    }
}
