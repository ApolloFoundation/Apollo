package com.apollocurrency.aplwallet.apl.core.dao;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;

import static com.apollocurrency.aplwallet.apl.core.dao.MariaDBConfigs.DOCKER_IMAGE_NAME_VERSION;

/**
 * An attempt to reuse existing container for multiple tests.
 */
@Slf4j
public abstract class DbContainerBaseTest {

    @Container
    public static final GenericContainer mariaDBContainer;

    static {
        mariaDBContainer = new MariaDBContainer(DOCKER_IMAGE_NAME_VERSION)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(3306)
            .withReuse(true)
            .withNetwork(null)
            .withLabel("com.apollocurrency.aplwallet.apl", "testcontainer")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withCommand(MariaDBConfigs.getEnvs())
        ;

        mariaDBContainer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(mariaDBContainer::stop));
    }

}
