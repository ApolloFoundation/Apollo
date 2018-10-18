package com.apollocurrency.aplwallet.apl;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.apollocurrency.aplwallet.TestData;
import dto.SSEDataHolder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import util.TestUtil;
import util.WalletRunner;

public class BlockSSETest {
    private volatile boolean closeSource = false;
    private static WalletRunner runner = new WalletRunner(true);
    private static final Logger LOG = getLogger(BlockSSETest.class);
    private static final String url = "http://localhost:6876/blocks?account=";

    @AfterClass
    public static void tearDown() {
        runner.shutdown();
    }

    @BeforeClass
    public static void setUp() throws IOException {
        runner.run();
    }

    @Test(timeout = 180_000)
    public void testSSE() throws InterruptedException {
        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url + TestData.MAIN_RS);

        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(new Consumer<InboundSseEvent>() {
                volatile int curBlockHeight = 0;

                @Override
                public void accept(InboundSseEvent sseEvent) {
                    try {
                        LOG.debug("Got sseEvent: {}", sseEvent.readData());
                        SSEDataHolder sseDataHolder = TestUtil.getMAPPER().readValue(sseEvent.readData(), SSEDataHolder.class);
                        Assert.assertNotNull(sseDataHolder);
                        Assert.assertEquals(sseDataHolder.getAccount().getAccount(), 9211698109297098287L);

                        Assert.assertTrue("Number of transactions should be eq or less than 10", sseDataHolder.getTransactions().size() <= 10);
                        Assert.assertTrue("Number of currencies should be eq or less than 3", sseDataHolder.getCurrencies().size() <= 3);
                        Assert.assertTrue("Number of assets should be eq or less than 3", sseDataHolder.getAssets().size() <= 3);
                        Assert.assertTrue("Number of purchases should be eq or less than 3", sseDataHolder.getPurchases().size() <= 10);
                        if (curBlockHeight == 0) {

                            curBlockHeight = sseDataHolder.getBlock().getHeight();
                        } else {
                            Assert.assertTrue("Height is not changed!",
                                    curBlockHeight < sseDataHolder.getBlock().getHeight()
                            );
                            closeSource = true;
                        }
                    }
                    catch (IOException e) {
                        LOG.error(e.toString(), e);
                        Assert.fail("Unable to parse sse json");
                    }
                }
            }, (e) -> {Assert.fail("SSE error"); LOG.error(e.toString(), e);});
            source.register(System.out::println);
            source.open();
            while (!closeSource) {
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    @Test
    public void testSSEFail() throws InterruptedException {
        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url);

        Consumer consumer = Mockito.mock(Consumer.class);
        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(consumer);
            source.open();
            TimeUnit.SECONDS.sleep(10);
            Mockito.verify(consumer, never()).accept(any());
        }
    }
}

