package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.Parameters;
import com.apollocurrrency.aplwallet.inttest.model.RequestType;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.addParameters;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.httpCallGet;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.httpCallPost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//@RunWith(JUnitPlatform.class)
@DisplayName("Disabled Test")
public class TestSmokeAPI extends TestBaseOld {

    private static ObjectMapper mapper = new ObjectMapper();
    private TestConfiguration testConfiguration;

    public TestSmokeAPI() {
        this.testConfiguration = TestConfiguration.getTestConfiguration();
    }

    @Test
    @Disabled
    public void verifyCountOfActivePeers() throws IOException {
        long etalonPeerBlockHeight;
        RetryPolicy retryPolicy = new RetryPolicy()
            .retryWhen(null)
            .withMaxRetries(10)
            .withDelay(5, TimeUnit.SECONDS);

        //Verify count of peers
        List<String> peers = getPeers();
        assertTrue("Peer counts < 3", peers.size() >= 3);

        //Verify transaction in block
        String transactionIndex = sendMoney(testConfiguration.getStandartWallet(), 200000000, 100000000).getTransaction();
        String blockIndex = Failsafe.with(retryPolicy).get(() -> verifyTransactionInBlock(transactionIndex, null));
        assertNotNull("Transaction don't added to block", blockIndex);

        //Verifu height
        etalonPeerBlockHeight = getLastBlock(null);
        verifyBlockHeighOnPeers(etalonPeerBlockHeight, peers);

        //Verify transactionIndex on peers
        for (int i = 0; i < peers.size(); i++) {
            int finalI = i;
            String blockIndexOnPeer = Failsafe.with(retryPolicy).get(() -> verifyTransactionInBlock(transactionIndex, String.valueOf(peers.get(finalI))));
            System.out.println("Block Index: " + blockIndexOnPeer + " peer" + peers.get(i));
            assertEquals(blockIndex, blockIndexOnPeer);
        }

        RetryPolicy retryPolicy2 = new RetryPolicy()
            .retryWhen(false)
            .withMaxRetries(5)
            .withDelay(5, TimeUnit.SECONDS);
        Failsafe.with(retryPolicy2).get(() -> getLastBlock(null) > etalonPeerBlockHeight);

        long newEtalonPeerBlockHeight = getLastBlock(null);
        verifyBlockHeighOnPeers(newEtalonPeerBlockHeight, peers);
    }


    private CreateTransactionResponse sendMoney(Wallet wallet, int moneyCount, int fee) throws IOException {
        addParameters(RequestType.requestType, RequestType.sendMoney);
        addParameters(Parameters.recipient, wallet.getUser());
        addParameters(Parameters.amountNQT, moneyCount);
        addParameters(Parameters.account, wallet.getUser());
        addParameters(Parameters.secretPhrase, wallet.getPass());
        addParameters(Parameters.feeNQT, fee);
        addParameters(Parameters.deadline, 1400);

        Response response = httpCallPost();
        assertEquals(200, response.code());
        return mapper.readValue(response.body().string().toString(), CreateTransactionResponse.class);
    }

    private String verifyTransactionInBlock(String transaction, String peerURL) throws IOException {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        Response response;

        if (peerURL == null)
            response = httpCallGet();
        else
            response = httpCallGet(peerURL);
        assertEquals(200, response.code());
        String rs = response.body().string().toString();
        return mapper.readValue(rs, TransactionDTO.class).getBlock();
    }

    private long getLastBlock(String peerURL) throws IOException {
        addParameters(RequestType.requestType, RequestType.getBlocks);
        addParameters(Parameters.lastIndex, 0);
        Response response;
        if (peerURL == null)
            response = httpCallGet();
        else
            response = httpCallGet(peerURL);
        assertEquals(200, response.code());

        BlockListInfoResponse resp = mapper.readValue(response.body().string().toString(), BlockListInfoResponse.class);
        //  return resp.blocks[0].height;
        return resp.getBlocks().get(0).getHeight();
    }

    private void verifyBlockHeighOnPeers(long etalonPeerBlockHeight, List<String> peers) throws IOException {
        for (int i = 0; i < peers.size(); i++) {
            long blockID = getLastBlock(String.valueOf(peers.get(i)));
            System.out.println("Heigh block: " + blockID + " On peer " + peers.get(i));
            assertTrue("Peer: " + peers.get(i), blockID == etalonPeerBlockHeight);
        }
    }
}
