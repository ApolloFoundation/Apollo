package com.apollocurrency.aplwallet.apl.db.updater;

import com.apollocurrency.aplwallet.apl.db.DbContainerBaseTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mariadb.jdbc.MariaDbDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("slow")
@Slf4j
class ShardAllScriptsDBUpdaterTest extends DbContainerBaseTest {

    public static final String mariadbTestUrl = "jdbc:mariadb://localhost:3306/testdb";
    public static final String mariadbUserName = "testuser";
    public static final String mariadbUserPass = "testpass";

    private ShardAllScriptsDBUpdater dbUpdater;
    private String dbType;
    MariaDbDataSource mariaDbDataSource;
    private List<String> tablesToDrop = List.of("block", "transaction", "option", "flyway_schema_history");

    @BeforeAll
    static void beforeAll() {
        log.info("Connect to the MariaDb using {}", mariadbTestUrl);
    }

    @BeforeEach
    void setUp() {
    }

    @SneakyThrows
    @AfterEach
    void tearDown() {
        if (dbType.equalsIgnoreCase("mariadb")) {
            int dropped = 0;
            mariaDbDataSource = new MariaDbDataSource(mariadbTestUrl);
            mariaDbDataSource.setUser(mariadbUserName);
            mariaDbDataSource.setPassword(mariadbUserPass);
            try (Connection con = mariaDbDataSource.getConnection()) {
                dropped = cleanUp(con, tablesToDrop);
            } catch (Exception e) {
                log.error("Connect error to MariaDb", e);
            }
            assertEquals(4, dropped);
        }
    }

    @Test
    void migrateH2(@TempDir Path tempDir) {
        dbType = "h2";
        MigrationParams params = new MigrationParams(
            "jdbc:h2:file:/" + tempDir.toAbsolutePath(), dbType, "test", "test");
        dbUpdater = new ShardAllScriptsDBUpdater();
        dbUpdater.update(params);
    }

    @Test
    void migrateMariaDb() {
        dbType = "mariadb";
        MigrationParams params = new MigrationParams(
            mariadbTestUrl, dbType, mariadbUserName, mariadbUserPass);
        dbUpdater = new ShardAllScriptsDBUpdater();
        dbUpdater.update(params);
    }

    public int cleanUp(Connection connection, List<String> tables) {
        int deletedTransactionCount = 0;
        for (String tableName : tables) {
            try (PreparedStatement statement = connection.prepareStatement(
                "DROP TABLE IF EXISTS `" + tableName.toLowerCase() + "`")) {
                statement.executeUpdate();
                deletedTransactionCount++;
            } catch (SQLException e) {
                log.error("Unable to delete db entries! ", e);
                return deletedTransactionCount;
            }
        }
        return deletedTransactionCount;
    }
}