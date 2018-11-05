package com.apollocurrency.aplwallet.apl.http;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;
import dto.SSEDataHolder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
        EventHandler eventHandler = new BlockSSEEventHandler();
        String targetUrl = String.format(url + TestConstants.MAIN_ACCOUNT.getAccountRS());
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(targetUrl));

        try (EventSource eventSource = builder.build()) {
            eventSource.setReconnectionTimeMs(300);
            eventSource.start();

            while (!closeSource) {
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    @Test
    public void testSSEFail() throws Exception {
        EventHandler eventHandler = new BlockSSEEventHandler();
        EventHandler spiedEventHandler = spy(eventHandler);
        EventSource.Builder builder = new EventSource.Builder(spiedEventHandler, URI.create(url));

        try (EventSource eventSource = builder.build()) {
            eventSource.setReconnectionTimeMs(300);
            eventSource.start();
            TimeUnit.SECONDS.sleep(10);
            verify(spiedEventHandler, never()).onMessage(anyString(), any(MessageEvent.class));
        }
    }


    private class BlockSSEEventHandler implements EventHandler {
        volatile int curBlockHeight = 0;

        @Override
        public void onOpen() throws Exception {
            System.out.println("onOpen");
        }

        @Override
        public void onClosed() throws Exception {
            System.out.println("onClosed");
        }

        @Override
        public void onMessage(String event, MessageEvent messageEvent) throws Exception {
            try {
                LOG.debug("Got sseEvent: {}-{}", event, messageEvent.getData());
                SSEDataHolder sseDataHolder = TestUtil.getMAPPER().readValue(messageEvent.getData(), SSEDataHolder.class);
                Assert.assertNotNull(sseDataHolder);
                Assert.assertEquals(sseDataHolder.getAccount().getAccountRS(), Convert.rsAccount(9211698109297098287L));

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
                    catch(IOException e){
                    LOG.error(e.toString(), e);
                    Assert.fail("Unable to parse sse json");
                }
            }

            @Override
            public void onComment (String comment) throws Exception {
                System.out.println("onComment");
            }

            @Override
            public void onError (Throwable t){
                System.out.println("onError: " + t);
            }

        }
    }

