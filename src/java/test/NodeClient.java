package test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static test.TestUtil.createURI;
import static test.TestUtil.getMAPPER;

public class NodeClient {
    public static final Long DEFAULT_FEE = 100_000_000L; //1 APL
    public static final Long DEFAULT_AMOUNT = 100_000_000L; //1 APL
    public static final Long DEFAULT_DEADLINE = 60L; //1 hour
    private static final Logger LOG = getLogger(NodeClientTestMainnet.class);
    private static final int REQUEST_TIMEOUT = 15;
    private static final HttpClient CLIENT = new HttpClient(new SslContextFactory());
    private static final ObjectMapper MAPPER = getMAPPER();
    private static final JSONParser PARSER = new JSONParser();

    static {
        try {
            CLIENT.setConnectTimeout(15_000);
            CLIENT.start();
        }
        catch (Exception e) {
            LOG.error("Http CLIENT could not initialized.", e);
            System.exit(0);
        }
    }

    public String getJson(URI uri) {
        return getJson(uri, null);
    }

    public String getJson(URI uri, Map<String, String> params) {
        try {
            Request request = CLIENT.newRequest(uri)
                    .method(HttpMethod.GET)
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            if (params != null) {
                params.forEach(request::param);
            }
            ContentResponse response = request.send();
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new RuntimeException("Request is not successful! Status= " + response.getStatus());
            }
            return response.getContentAsString();
        }
        catch (Exception e) {
            LOG.error("Cannot perform getRequest to " + uri.toString(), e);
        }
        return null;
    }

    public String postJson(URI uri, Map<String, String> parameters, String bodyJson) {
        try {
            Request request = CLIENT.newRequest(uri)
                    .method(HttpMethod.POST)
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .content(new StringContentProvider(bodyJson, Charset.forName("utf-8")));
            parameters.forEach(request::param);
            ContentResponse response = request.send();
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new RuntimeException("Request is not successful! Status= " + response.getStatus());
            }
            return response.getContentAsString();
        }
        catch (Exception e) {
            LOG.error("Cannot perform postRequest to " + uri.toString() + "/" + uri.getPath(), e);
        }
        return null;
    }

    /**
     * curl -X POST -d "requestType=sendMoney&secretPhrase=dont know secret phrase hungry&recipient=APL-R3YT-LZ35-PS5X-3NMHR&amountNQ
     * T=100000000&feeNQT=100000000&deadline=60" http://localhost:7876/apl
     */

    public String getPeers(String url) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getPeers");
        params.put("active", "true");
        params.put("state", "CONNECTED");
        params.put("includePeerInfo", "true");
        URI uri = createURI(url);
        return getJson(uri, params);
    }

    public List<Peer> getPeersList(String url) throws IOException {
        String json = getPeers(url);
        JsonNode root = MAPPER.readTree(json);
        JsonNode peersArray = root.get("peers");
        return MAPPER.readValue(peersArray.toString(), new TypeReference<List<Peer>>() {});
    }

    public String getBlocks(String url, int from, int to, boolean includeTransactions, Long timestamp) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getBlocks");
        params.put("firstIndex", String.valueOf(from));
        params.put("lastIndex", String.valueOf(to));
        if (timestamp != null) {
            params.put("timestamp", timestamp.toString());
        }
        params.put("includeTransactions", String.valueOf(includeTransactions));
        URI uri = createURI(url);
        return getJson(uri, params);
    }

    public String getBlocks(String url) {
        return getBlocks(url, 0, 4, false, null); //last 5 blocks
    }


    public Long getBlockchainHeight(String url) throws Exception {
        String blocks = getBlocks(url);
        return (Long) ((JSONObject) ((JSONArray) ((JSONObject) PARSER.parse(blocks)).get("blocks")).get(0)).get("height");
    }

    public String getAccountTransactions(String url, String rsAddress) {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getBlockchainTransactions");
        params.put("account", rsAddress);
        params.put("includeTransactions", "true");
        params.put("includeExecutedPhased", "false");
        return getJson(uri, params);
    }

    public List<Transaction> getAccountTransactionsList(String url, String rsAddress) throws ParseException, IOException {
        String accountTransactions = getAccountTransactions(url, rsAddress);
        JsonNode root = MAPPER.readTree(accountTransactions);
        JsonNode transactionsArray = root.get("transactions");
        List<Transaction> transactionsList = MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<Transaction>>() {});
        return transactionsList;
    }

    public List<String> getPeersIPs(String url) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getPeers");
        params.put("state", "CONNECTED");
        URI uri = createURI(url);
        String json = getJson(uri, params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode peersArray = root.get("peers");
        return MAPPER.readValue(peersArray.toString(), new TypeReference<List<String>>() {});
    }

    public int getPeersCount(String url) throws ParseException {
        String peers = getPeers(url);
        return ((JSONArray) ((JSONObject) PARSER.parse(peers)).get("peers")).size();

    }

    public String getBlockTransactions(String url, Long height) throws ParseException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getBlock");
        params.put("height", height.toString());
        params.put("includeTransactions", "true");
        params.put("includeExecutedPhased", "true");
        URI uri = createURI(url);
        String json = getJson(uri, params);
        JSONArray transactions = (JSONArray) ((JSONObject) PARSER.parse(json)).get("transactions");
        return transactions.toJSONString();
    }

    public List<Transaction> getBlockTransactionsList(String url, Long height) throws ParseException, IOException {
        String blockTransactions = getBlockTransactions(url, height);
        List<Transaction> transactionsList = MAPPER.readValue(blockTransactions, new TypeReference<List<Transaction>>() {});
        return transactionsList;
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT, Long deadline) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "sendMoney");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("recipient", recipient);
        parameters.put("amountNQT", amountNQT.toString());
        parameters.put("feeNQT", feeNQT.toString());
        parameters.put("deadline", deadline.toString());
        return postJson(createURI(url), parameters, "");
    }

    public Transaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT, Long deadline) throws IOException, ParseException {
        String json = sendMoney(url, secretPhrase, recipient, amountNQT, feeNQT, deadline);
        String transactionJSON = ((JSONObject) ((JSONObject) PARSER.parse(json)).get("transactionJSON")).toJSONString();
        return MAPPER.readValue(transactionJSON, Transaction.class);
    }

    private String sendMoneyPrivate(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT, Long deadline) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "sendMoneyPrivate");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("recipient", recipient);
        parameters.put("amountNQT", amountNQT.toString());
        parameters.put("feeNQT", feeNQT.toString());
        parameters.put("deadline", deadline.toString());
        return postJson(createURI(url), parameters, "");
    }

    public Transaction sendMoneyPrivateTransaction(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT, Long deadline) throws IOException, ParseException {
        String json = sendMoneyPrivate(url, secretPhrase, recipient, amountNQT, feeNQT, deadline);
        String transactionJSON = ((JSONObject) ((JSONObject) PARSER.parse(json)).get("transactionJSON")).toJSONString();
        return MAPPER.readValue(transactionJSON, Transaction.class);
    }

    public Transaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT) throws IOException, ParseException {
        return sendMoneyTransaction(url, secretPhrase, recipient, amountNQT, feeNQT, DEFAULT_DEADLINE);
    }

    public Transaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountNQT) throws IOException, ParseException {
        return sendMoneyTransaction(url, secretPhrase, recipient, amountNQT, DEFAULT_FEE);
    }

    public List<Block> getBlocksList(String url, boolean includeTransactions, Long timestamp) throws IOException {
        String json = getBlocks(url, 0, 4, includeTransactions, timestamp);
        JsonNode root = MAPPER.readTree(json);
        JsonNode blocksArray = root.get("blocks");
        return MAPPER.readValue(blocksArray.toString(), new TypeReference<List<Block>>() {});
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountNQT) {
        return sendMoney(url, secretPhrase, recipient, amountNQT, DEFAULT_FEE);
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountNQT, Long feeNQT) {
        return sendMoney(url, secretPhrase, recipient, amountNQT, feeNQT, DEFAULT_DEADLINE);
    }

    public String sendMoney(String url, String secretPhrase, String recipient) {
        return sendMoney(url, secretPhrase, recipient, DEFAULT_AMOUNT);
    }

    public Transaction getTransaction(String url, String fullHash) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getTransaction");
        parameters.put("fullHash", fullHash);
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, Transaction.class);
    }

    public List<Transaction> getPrivateBlockchainTransactionsList(String url, String secretPhrase, Long height) throws Exception {
        String json = getPrivateBlockchainTransactionsJson(url, secretPhrase, height);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("transactions");
        return MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<Transaction>>() {});
    }

    public String getPrivateBlockchainTransactionsJson(String url, String secretPhrase, Long height) {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getPrivateBlockchainTransactions");
        params.put("secretPhrase", secretPhrase);
        if (height != null) {
            params.put("height", height.toString());
        }
        return getJson(uri, params);
    }

    public List<LedgerEntry> getAccountLedger(String url, String rsAddress, boolean includeTransactions) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getAccountLedger");
        parameters.put("account", rsAddress);
        parameters.put("includeTransactions", String.valueOf(includeTransactions));
        String json = getJson(createURI(url), parameters);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("entries");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<LedgerEntry>>() {});
    }

    public List<LedgerEntry> getPrivateAccountLedger(String url, String secretPhrase, boolean includeTransactions) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getPrivateAccountLedger");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("includeTransactions", String.valueOf(includeTransactions));
        String json = getJson(createURI(url), parameters);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("entries");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<LedgerEntry>>() {});
    }

    public Transaction getPrivateTransaction(String url, String secretPhrase, String fullHash, String transactionId) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getPrivateTransaction");
        parameters.put("secretPhrase", secretPhrase);
        if (transactionId != null && !transactionId.isEmpty()) {
            parameters.put("transaction", String.valueOf(transactionId));
        } else if (fullHash != null && !fullHash.isEmpty()) {
            parameters.put("fullHash", fullHash);
        } else {
            throw new RuntimeException("Both fullHash and transactionId are not provided");
        }
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, Transaction.class);
    }
}
