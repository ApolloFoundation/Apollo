/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;
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

import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;

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
        findAndExecute(schemaScriptPath, "Schema");
    }

    private void loadSqlAndExecute(URI file) {
        byte[] bytes = readAllBytes(file);

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

    private byte[] readAllBytes(URI file) {
        try (InputStream inputStream = new FileInputStream(new File(file))) {
            return inputStream.readAllBytes();
        }
        catch (IOException exception) {
            throw new RuntimeException("Unable to read file " + file);
        }
    }


    public void populateDb() {
        findAndExecute(dataScriptPath, "Data");
    }

    public void findAndExecute(String resource, String name) {
        if (StringUtils.isNotBlank(resource)) {
            URI resourceUri = findResource(resource, name);
            loadSqlAndExecute(resourceUri);
        }
    }

    public URI findResource(String resourcePath, String name) {
        StringValidator.requireNonBlank(resourcePath, "db resource " + name);
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        Objects.requireNonNull(resource, "Resource not found  in classpath: " + resourcePath);
        try {
            return resource.toURI();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }


}
