/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

@Slf4j
public class DbPopulator {

    private DataSource basicDataSource;
    private String schemaScriptPath;
    private String dataScriptPath;

    public DbPopulator(DataSource db, String schemaScriptPath, String dataScriptPath) {
        this.basicDataSource = db;
        this.schemaScriptPath = schemaScriptPath;
        this.dataScriptPath = dataScriptPath;
    }

    public void initDb() {
        try {
            Path schemaDbPath = Paths.get(getClass().getClassLoader().getResource(schemaScriptPath).toURI());
            loadSqlAndExecute(schemaDbPath);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Unable to load sql commands", e);
        }
    }

    private void loadSqlAndExecute(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        int appliedResults = 0;
        StringTokenizer tokenizer = new StringTokenizer(new String(bytes), ";");
        while (tokenizer.hasMoreElements()) {
            String sqlCommand = tokenizer.nextToken();
            try (Connection con = basicDataSource.getConnection();
                 Statement stm = con.createStatement()) {
                if (sqlCommand.trim().length() != 0 && !sqlCommand.trim().startsWith("--")) {
                    appliedResults += stm.executeUpdate(sqlCommand);
                    con.commit();
                }
            } catch (SQLException e) {
                log.error("Error for: {}", sqlCommand);
                throw new RuntimeException(e.toString(), e);
            }
        }
        log.trace("Applied '{}' test data commands", appliedResults);
    }

    public void populateDb() {
        try {
            Path dataDbPath = Paths.get(getClass().getClassLoader().getResource(dataScriptPath).toURI());
            loadSqlAndExecute(dataDbPath);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Unable to load sql commands", e);
        }
    }


}
