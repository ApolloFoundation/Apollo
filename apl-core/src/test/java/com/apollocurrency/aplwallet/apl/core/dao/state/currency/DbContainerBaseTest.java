package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;

/**
 * An attempt to reuse existing container for multiple tests.
 */
 @Slf4j
public abstract class DbContainerBaseTest {

    @Container
    public static final GenericContainer mariaDBContainer;

    static {
        mariaDBContainer = new MariaDBContainer("mariadb:10.5")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(3306)
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(log));

        mariaDBContainer.start();
    }
}
