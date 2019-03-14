/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.testutil.DbManipulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;

public class DbTest {
    static DbManipulator manipulator;
    public DbTest(Path path, String password, String user) {
        manipulator = new DbManipulator(path, user, password);
    }
    public DbTest() {
        manipulator = new DbManipulator();
    }

    @BeforeEach
    public void setUp() throws Exception {
        manipulator.init();
        manipulator.populate();
    }

    @AfterEach
    public void tearDown() throws Exception {
        manipulator.shutdown();
        Path tempDbFile = manipulator.getTempDbFile();
        if (tempDbFile != null) {
            Files.delete(Paths.get(tempDbFile.toAbsolutePath().toString() + ".h2.db"));
        }
    }

    public DataSource getDataSource() {
        return manipulator.getDataSource();
    }
}
