/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.core.db.DbTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class H2DbInfoExtractorTest extends DbTest {
    private static Path path;
    private H2DbInfoExtractor h2DbInfoExtractor = new H2DbInfoExtractor("user", "pass");

    static {
        try {
            path = Files.createTempFile("test", "h2");
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public H2DbInfoExtractorTest() throws IOException {
        super(path, "pass", "user");
    }

    @Test
    public void testGetHeight() {
        int height = h2DbInfoExtractor.getHeight(path.toString());
        Assertions.assertEquals(104671, height);

    }

    @Test
    public void testGetPath() {
        String path = H2DbInfoExtractorTest.path.toAbsolutePath().toString();
        Assertions.assertEquals(Paths.get(path + ".h2.db"), h2DbInfoExtractor.getPath(path));
    }
}
