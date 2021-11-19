/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.StringTokenizer;

@Slf4j
public class DbPopulator {
    private final String schemaScriptPath;
    private final String dataScriptPath;

    public DbPopulator(String schemaScriptPath, String dataScriptPath) {
        this.schemaScriptPath = schemaScriptPath;
        this.dataScriptPath = dataScriptPath;
    }

    public void initDb(TransactionalDataSource db) {
        findAndExecute(db, schemaScriptPath, "Schema");
    }

    public void executeUseDbSql(TransactionalDataSource dataSource) {
        Objects.requireNonNull(dataSource.getDbIdentity(), "shardName is NULL");
        try (Connection con = dataSource.getConnection();
             Statement stm = con.createStatement()) {
            stm.executeUpdate(String.format("use %s;", dataSource.getDbIdentity().get()));
            con.commit();
        } catch (SQLException e) {
            log.error("Error executing USE shard command", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void loadSqlAndExecute(TransactionalDataSource dataSource, URI file) {
        int appliedResults = 0;
        StringTokenizer tokenizer = new StringTokenizer(new String(readAllBytes(file)), ";");

        try (Connection con = dataSource.getConnection();
             Statement stm = con.createStatement()) {
            while (tokenizer.hasMoreElements()) {
                String sqlCommand = tokenizer.nextToken();
                if (sqlCommand.trim().length() != 0) {
                    stm.addBatch(sqlCommand);
                }
            }
            stm.executeBatch();
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        log.trace("Applied '{}' test data commands into db=[{}]", appliedResults, (dataSource).getDbIdentity());
    }

    private byte[] readAllBytes(URI file) {
        try (InputStream inputStream = new FileInputStream(new File(file))) {
            return inputStream.readAllBytes();
        } catch (IOException exception) {
            throw new RuntimeException("Unable to read file " + file);
        }
    }


    public void populateDb(TransactionalDataSource dataSource) {
        findAndExecute(dataSource, dataScriptPath, "Data");
    }

    public void findAndExecute(TransactionalDataSource db, String resource, String name) {
        if (StringUtils.isNotBlank(resource)) {
            URI resourceUri = findResource(resource, name);
            loadSqlAndExecute(db, resourceUri);
        }
    }

    public URI findResource(String resourcePath, String name) {
        StringValidator.requireNonBlank(resourcePath, "db resource " + name);
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        Objects.requireNonNull(resource, "Resource not found  in classpath: " + resourcePath);
        try {
            return resource.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }


}
