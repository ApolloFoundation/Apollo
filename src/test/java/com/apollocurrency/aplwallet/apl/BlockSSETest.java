package com.apollocurrency.aplwallet.apl;



import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import util.WalletRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.util.concurrent.TimeUnit;

public class BlockSSETest {
    private static WalletRunner runner = new WalletRunner(true);

    @AfterClass
    public static void tearDown() throws Exception {
        runner.shutdown();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        runner.run();
    }

    @Test
    public void testSSE() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:7876/blocks?account=9211698109297098287");

        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(System.out::println);
            source.open();
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }
    }
    }

