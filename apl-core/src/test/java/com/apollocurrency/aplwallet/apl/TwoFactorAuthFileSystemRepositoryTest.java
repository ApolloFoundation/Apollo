/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthFileSystemRepository;
import com.apollocurrency.aplwallet.apl.db.TwoFactorAuthRepository;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.junit.After;
import org.junit.Before;
import util.TestUtil;

public class TwoFactorAuthFileSystemRepositoryTest extends AbstractTwoFactorAuthRepositoryTest {

    private TwoFactorAuthRepository repository;
    private  Path repositoryPath;

    @Before
    public void setUp() throws IOException {
        repositoryPath = Files.createTempDirectory("test2faFSrepository");
        repository = new TwoFactorAuthFileSystemRepository(repositoryPath);
        setRepository(repository);
        String confirmedAccount = Convert.defaultRsAccount(TwoFactorAuthTestData.ENTITY1.getAccount());
        String unconfirmedAccount = Convert.defaultRsAccount(TwoFactorAuthTestData.ENTITY2.getAccount());
        JSON.writeJson(repositoryPath.resolve(confirmedAccount), TwoFactorAuthTestData.ENTITY1);
        JSON.writeJson(repositoryPath.resolve(unconfirmedAccount), TwoFactorAuthTestData.ENTITY2);
    }
    @After
    public  void tearDown() throws IOException {
        TestUtil.deleteDir(repositoryPath, (path)-> true);
    }
}
