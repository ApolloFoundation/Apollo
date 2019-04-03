/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

public class DbPopulator {
    private static final Logger LOG = getLogger(DbPopulator.class);

    private DataSource basicDataSource;
    private String schemaScriptPath;
    private String dataScriptPath;

    public DbPopulator(DataSourceWrapper db, String schemaScriptPath, String dataScriptPath) {
        this.basicDataSource = db;
        this.schemaScriptPath = schemaScriptPath;
        this.dataScriptPath = dataScriptPath;
    }

    public void initDb() {
        try {
            Path schemaDbPath = Paths.get(getClass().getClassLoader().getResource(schemaScriptPath).toURI());
            loadSqlAndExecute(schemaDbPath);
        }
        catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Unable to load sql commands", e);
        }
    }

    private void loadSqlAndExecute(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        StringTokenizer tokenizer = new StringTokenizer(new String(bytes), ";");
        while (tokenizer.hasMoreElements()) {
            String sqlCommand = tokenizer.nextToken();
            try (Connection con = basicDataSource.getConnection();
                 Statement stm = con.createStatement()) {
                stm.executeUpdate(sqlCommand);
                con.commit();
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

    }

    public void populateDb() {
        try {
            Path dataDbPath = Paths.get(getClass().getClassLoader().getResource(dataScriptPath).toURI());
            loadSqlAndExecute(dataDbPath);
        }
        catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Unable to load sql commands", e);
        }
    }


}
