package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.addParameters;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.httpCallGet;
import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.httpCallPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//@RunWith(JUnitPlatform.class)

public class TestSmokeAPI extends TestBase {

    private TestConfiguration testConfiguration;
    private  static ObjectMapper mapper = new ObjectMapper(); 
    
    public TestSmokeAPI()
    {
        this.testConfiguration = TestConfiguration.getTestConfiguration();
    }

    @Test
    public void verifyCountOfActivePeers() throws IOException {
          long etalonPeerBlockHeight;
           RetryPolicy retryPolicy = new RetryPolicy()
                .retryWhen(null)
                .withMaxRetries(10)
                .withDelay(30, TimeUnit.SECONDS);

        //Verify count of peers
        String [] peers = getPeers();
        assertTrue("Peer counts < 3",  peers.length >= 3);

        //Verify transaction in block
        String transactionIndex =  sendMoney(testConfiguration.getTestUser(),200000000, 100000000).transaction;
        String blockIndex = Failsafe.with(retryPolicy).get(() -> verifyTransactionInBlock(transactionIndex,null));
        assertNotNull("Transaction don't added to block", blockIndex);

        //Verifu height
        etalonPeerBlockHeight = getLastBlock(null);
        verifyBlockHeighOnPeers(etalonPeerBlockHeight,peers);

        //Verify transactionIndex on peers
        for (int i = 0; i < peers.length ; i++) {
                int finalI = i;
                String blockIndexOnPeer = Failsafe.with(retryPolicy).get(() -> verifyTransactionInBlock(transactionIndex,peers[finalI]));
                System.out.println("Block Index: "+blockIndexOnPeer+" peer"+peers[i]);
                assertEquals(blockIndex,blockIndexOnPeer);
        }

        RetryPolicy retryPolicy2 = new RetryPolicy()
                .retryWhen(false)
                .withMaxRetries(5)
                .withDelay(5, TimeUnit.SECONDS);
        Failsafe.with(retryPolicy2).get(() -> getLastBlock(null) > etalonPeerBlockHeight );

        long newEtalonPeerBlockHeight = getLastBlock(null);
        verifyBlockHeighOnPeers(newEtalonPeerBlockHeight,peers);
    }




    private CreateTransactionResponse sendMoney(String recipient, int moneyCount, int fee) throws IOException {
        addParameters(RequestType.requestType, RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountNQT, moneyCount);
        addParameters(Parameters.secretPhrase, testConfiguration.getSecretPhrase());
        addParameters(Parameters.feeNQT, fee);
        addParameters(Parameters.deadline, 1400);

        Response response = httpCallPost();
        assertEquals(200, response.code());
        return   mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }

    private String verifyTransactionInBlock(String transaction, String peerURL) throws IOException {
        addParameters(RequestType.requestType,RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        Response response;

        if (peerURL == null)
            response = httpCallGet();
        else
            response = httpCallGet(peerURL);
        assertEquals( 200, response.code());
        String rs = response.body().string().toString();
        return   mapper.readValue(rs, TransactionDTO.class).block;
    }

    private long getLastBlock(String peerURL) throws IOException {
        addParameters(RequestType.requestType,RequestType.getBlocks);
        addParameters(Parameters.lastIndex, 0);
        Response response;
        if (peerURL == null)
            response = httpCallGet();
        else
            response = httpCallGet(peerURL);
        assertEquals(200, response.code());

        BlockListInfoResponse resp =   mapper.readValue(response.body().string().toString(), BlockListInfoResponse.class);
      //  return resp.blocks[0].height;
         return resp.blocks.get(0).height;
    }

    private void verifyBlockHeighOnPeers(long etalonPeerBlockHeight, String[] peers) throws IOException {
        for (int i = 0; i < peers.length ; i++) {
                long blockID = getLastBlock(peers[i]);
                System.out.println("Heigh block: " + blockID + " On peer " + peers[i]);
                assertTrue("Peer: " + peers[i], blockID == etalonPeerBlockHeight);
        }
    }
}
