/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;
import static util.TestUtil.createURI;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.Account2FA;
import dto.AccountWithKey;
import dto.AccountsStatistic;
import dto.Block;
import dto.ChatInfo;
import dto.ForgingDetails;
import dto.JSONTransaction;
import dto.LedgerEntry;
import dto.NextGenerators;
import dto.Peer;
import dto.TwoFactorAuthAccountDetails;
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
import util.TestUtil;

public class NodeClient {
    public static final Long DEFAULT_FEE = 100_000_000L; //1 APL
    public static final Long DEFAULT_AMOUNT = 100_000_000L; //1 APL
    public static final Long DEFAULT_DEADLINE = 60L; //1 hour
    private static final Logger LOG = getLogger(NodeClient.class);
    private static final int REQUEST_TIMEOUT = 15;
    private static final HttpClient CLIENT = new HttpClient(new SslContextFactory());
    private static final ObjectMapper MAPPER = TestUtil.getMAPPER();
    private static final JSONParser PARSER = new JSONParser();

    static {
        try {
            CLIENT.setConnectTimeout(15_000);
            CLIENT.start();
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            LOG.error("Cannot perform postRequest to " + uri.toString() + "/" + uri.getPath(), e);
        }
        return null;
    }

    /**
     * curl -X POST -d "requestType=sendMoney&secretPhrase=dont know secret phrase hungry&recipient=APL-R3YT-LZ35-PS5X-3NMHR&amountNQ
     * T=100000000&feeATM=100000000&deadline=60" http://localhost:7876/apl
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
        return MAPPER.readValue(peersArray.toString(), new TypeReference<List<Peer>>() {
        });
    }

    public String getBlocks(String url, int from, int to, boolean includeTransactions, Long timestamp) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getBlocks");
        putPagination(params, from, to);
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


    public int getBlockchainHeight(String url) throws Exception {
        String blocks = getBlocks(url);
        return Integer.parseInt(((JSONObject) ((JSONArray) ((JSONObject) PARSER.parse(blocks)).get("blocks")).get(0)).get("height").toString());
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

    public List<JSONTransaction> getAccountTransactions(String url, String rsAddress, int from, int to, int type, int subtype) throws IOException {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getBlockchainTransactions");
        params.put("account", rsAddress);
        params.put("includeTransactions", "true");
        putPagination(params, from, to);
        if (type >= 0) {
            params.put("type", String.valueOf(type));
        }
        if (subtype >= 0) {
            params.put("subtype", String.valueOf(subtype));
        }
        String json = getJson(uri, params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("transactions");
        try {
            return MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<JSONTransaction>>() {});
        }
        catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    public List<JSONTransaction> getAccountTransactionsList(String url, String rsAddress) throws ParseException, IOException {
        String accountTransactions = getAccountTransactions(url, rsAddress);
        JsonNode root = MAPPER.readTree(accountTransactions);
        JsonNode transactionsArray = root.get("transactions");
        List<JSONTransaction> transactionsList = MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<JSONTransaction>>() {});
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
        return MAPPER.readValue(peersArray.toString(), new TypeReference<List<String>>() {
        });
    }

    public int getPeersCount(String url) throws ParseException {
        String peers = getPeers(url);
        return ((JSONArray) ((JSONObject) PARSER.parse(peers)).get("peers")).size();

    }

    public String getBlockTransactions(String url, int height) throws ParseException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getBlock");
        params.put("height", String.valueOf(height));
        params.put("includeTransactions", "true");
        params.put("includeExecutedPhased", "true");
        URI uri = createURI(url);
        String json = getJson(uri, params);
        JSONArray transactions = (JSONArray) ((JSONObject) PARSER.parse(json)).get("transactions");
        return transactions.toJSONString();
    }

    public List<JSONTransaction> getBlockTransactionsList(String url, int height) throws ParseException, IOException {
        String json = getBlockTransactions(url, height);
        return MAPPER.readValue(json, new TypeReference<List<JSONTransaction>>() {});
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM, Long deadline) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "sendMoney");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("recipient", recipient);
        parameters.put("amountATM", amountATM.toString());
        parameters.put("feeATM", feeATM.toString());
        parameters.put("deadline", deadline.toString());
        return postJson(createURI(url), parameters, "");
    }

    public JSONTransaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM, Long deadline) throws IOException, ParseException {
        String json = sendMoney(url, secretPhrase, recipient, amountATM, feeATM, deadline);
        String transactionJSON;
        try {
            transactionJSON = ((JSONObject) ((JSONObject) PARSER.parse(json)).get("transactionJSON")).toJSONString();
        }
        catch (Exception e) {
            throw new RuntimeException("Unsuccessful request:" + json, e);
        }
        return MAPPER.readValue(transactionJSON, JSONTransaction.class);
    }

    private String sendMoneyPrivate(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM, Long deadline) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "sendMoneyPrivate");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("recipient", recipient);
        parameters.put("amountATM", amountATM.toString());
        parameters.put("feeATM", feeATM.toString());
        parameters.put("deadline", deadline.toString());
        return postJson(createURI(url), parameters, "");
    }

    public JSONTransaction sendMoneyPrivateTransaction(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM,
                                                      Long deadline) throws IOException, ParseException {
        String json = sendMoneyPrivate(url, secretPhrase, recipient, amountATM, feeATM, deadline);
        String transactionJSON = ((JSONObject) ((JSONObject) PARSER.parse(json)).get("transactionJSON")).toJSONString();
        return MAPPER.readValue(transactionJSON, JSONTransaction.class);
    }

    public JSONTransaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM) throws IOException,
            ParseException {
        return sendMoneyTransaction(url, secretPhrase, recipient, amountATM, feeATM, DEFAULT_DEADLINE);
    }
    public JSONTransaction sendMoneyTransaction(String url, String secretPhrase, String recipientPublicKey, int deadline, String recipient,
                                                Long amountATM,
                                                Long feeATM) throws IOException,
            ParseException {
        Map<String, String> params = new HashMap<>();
        params.put("recipientPublicKey", recipientPublicKey);
        params.put("requestType", "sendMoney");
        params.put("recipient", recipient);
        params.put("amountATM", String.valueOf(amountATM));
        return sendTransaction(url, secretPhrase, feeATM, deadline, params);
    }

    public JSONTransaction sendMoneyTransaction(String url, String secretPhrase, String recipient, Long amountATM) throws IOException,
            ParseException {
        return sendMoneyTransaction(url, secretPhrase, recipient, amountATM, DEFAULT_FEE);
    }

    public List<Block> getBlocksList(String url, boolean includeTransactions, Long timestamp) throws IOException {
        String json = getBlocks(url, 0, 4, includeTransactions, timestamp);
        JsonNode root = MAPPER.readTree(json);
        JsonNode blocksArray = root.get("blocks");
        return MAPPER.readValue(blocksArray.toString(), new TypeReference<List<Block>>() {
        });
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountATM) {
        return sendMoney(url, secretPhrase, recipient, amountATM, DEFAULT_FEE);
    }

    public String sendMoney(String url, String secretPhrase, String recipient, Long amountATM, Long feeATM) {
        return sendMoney(url, secretPhrase, recipient, amountATM, feeATM, DEFAULT_DEADLINE);
    }

    public String sendMoney(String url, String secretPhrase, String recipient) {
        return sendMoney(url, secretPhrase, recipient, DEFAULT_AMOUNT);
    }

    public JSONTransaction getTransaction(String url, String fullHash) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getTransaction");
        parameters.put("fullHash", fullHash);
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, JSONTransaction.class);
    }

    public List<JSONTransaction> getPrivateBlockchainTransactionsList(String url, String secretPhrase, int height, Long firstIndex, Long lastIndex) throws Exception {
        String json = getPrivateBlockchainTransactionsJson(url, secretPhrase, height, firstIndex, lastIndex);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("transactions");
        return MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<JSONTransaction>>() {});
    }

    public List<JSONTransaction> getEncryptedPrivateBlockchainTransactionsList(String url, int height, Long firstIndex, Long lastIndex, String secretPhrase, String publicKey) throws Exception {
        String json = getEncryptedPrivateBlockchainTransactionsJson(url, height, firstIndex, lastIndex, null, publicKey);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("transactions");
        JsonNode serverPublicKey = root.get("serverPublicKey");
        if (transactionsArray == null || serverPublicKey == null) {
            throw new RuntimeException("Cannot find transactions or serverPublic key in json: " + root.toString());
        }
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), Convert.parseHexString(serverPublicKey.textValue()));
        List<JSONTransaction> transactions = new ArrayList<>();
        for (final JsonNode transactionJson: transactionsArray) {
            if (transactionJson.get("encryptedTransaction") != null) {
                JsonNode encryptedTransaction = transactionJson.get("encryptedTransaction");
                byte[] encryptedBytes = Convert.parseHexString(encryptedTransaction.textValue());
                byte[] decryptedData = Crypto.aesDecrypt(encryptedBytes, sharedKey);
                String decryptedJson = Convert.toString(decryptedData);
                JSONTransaction transaction = MAPPER.readValue(decryptedJson, JSONTransaction.class);
                transactions.add(transaction);
            } else {
                transactions.add(MAPPER.readValue(transactionJson.toString(), JSONTransaction.class));
            }
        }
        return transactions;
    }

    public String getPrivateBlockchainTransactionsJson(String url, String secretPhrase, int height, Long firstIndex, Long lastIndex) {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getPrivateBlockchainTransactions");
        params.put("secretPhrase", secretPhrase);
        if (height >= 0) {
            params.put("height", String.valueOf(height));
        }
        if (firstIndex != null) {
            params.put("firstIndex", firstIndex.toString());
        }
        if (lastIndex != null) {
            params.put("lastIndex", lastIndex.toString());
        }
        return getJson(uri, params);
    }

    public String getEncryptedPrivateBlockchainTransactionsJson(String url, int height, Long firstIndex, Long lastIndex, String secretPhrase, String publicKey) {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getPrivateBlockchainTransactions");

        if (publicKey != null) {
            params.put("publicKey", publicKey);
        } else if (secretPhrase != null) {
            params.put("secretPhrase", secretPhrase);
        }
        if (height >= 0) {
            params.put("height", String.valueOf(height));
        }
        if (firstIndex != null) {
            params.put("firstIndex", firstIndex.toString());
        }
        if (lastIndex != null) {
            params.put("lastIndex", lastIndex.toString());
        }
        return getJson(uri, params);
    }

    public List<LedgerEntry> getAccountLedger(String url, String rsAddress, boolean includeTransactions) throws Exception {
        return getAccountLedger(url, rsAddress, includeTransactions, 0, -1);
    }

    public List<LedgerEntry> getAccountLedger(String url, String rsAddress, boolean includeTransactions, int from, int to) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getAccountLedger");
        params.put("account", rsAddress);
        params.put("includeTransactions", String.valueOf(includeTransactions));
        putPagination(params, from, to);
        String json = getJson(createURI(url), params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("entries");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<LedgerEntry>>() {
        });
    }

    public List<JSONTransaction> getUnconfirmedTransactions(String url, String rsAddress, int from, int to) throws
            Exception {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getUnconfirmedTransactions");
        params.put("account", rsAddress);
        putPagination(params, from, to);
        String json = getJson(createURI(url), params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("unconfirmedTransactions");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<JSONTransaction>>() {});
    }
    public List<JSONTransaction> getPrivateUnconfirmedTransactions(String url, String secretPhrase, int from, int to) throws
            Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getPrivateUnconfirmedTransactions");
        parameters.put("secretPhrase", secretPhrase);
        putPagination(parameters, from, to);
        String json = getJson(createURI(url), parameters);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("unconfirmedTransactions");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<JSONTransaction>>() {});
    }

    public List<LedgerEntry> getPrivateAccountLedger(String url, String secretPhrase, boolean includeTransactions, int from, int to) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getPrivateAccountLedger");
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("includeTransactions", String.valueOf(includeTransactions));
        putPagination(parameters, from, to);
        String json = getJson(createURI(url), parameters);
        JsonNode root = MAPPER.readTree(json);
        JsonNode entriesArray = root.get("entries");
        return MAPPER.readValue(entriesArray.toString(), new TypeReference<List<LedgerEntry>>() {
        });
    }

    public JSONTransaction getPrivateTransaction(String url, String secretPhrase, String fullHash, String transactionId) throws IOException {
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
        return MAPPER.readValue(json, JSONTransaction.class);
    }

    public Block getBlock(String url, int height) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getBlock");
        params.put("height", String.valueOf(height));
        params.put("includeTransactions", "true");
        URI uri = createURI(url);
        String json = getJson(uri, params);
        return MAPPER.readValue(json, Block.class);
    }

    public LedgerEntry getAccountLedgerEntry(String url, Long ledgerId, Boolean includeTransaction) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getAccountLedgerEntry");
        parameters.put("ledgerId", ledgerId.toString());
        parameters.put("includeTransaction", includeTransaction.toString());
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, LedgerEntry.class);
    }

    public LedgerEntry getPrivateAccountLedgerEntry(String url, String secretPhrase, Long ledgerId, Boolean includeTransaction) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getPrivateAccountLedgerEntry");
        parameters.put("ledgerId", ledgerId.toString());
        parameters.put("secretPhrase", secretPhrase);
        parameters.put("includeTransaction", includeTransaction.toString());
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, LedgerEntry.class);
    }

    public List<dto.Account> getGenesisBalances(String url, int firstIndex, int lastIndex) throws IOException {
        String json = getGenesisBalancesJSON(url, firstIndex, lastIndex);
        JsonNode accountsNode = MAPPER.readTree(json).get("accounts");
        return MAPPER.readValue(accountsNode.toString(), new TypeReference<List<dto.Account>>(){});
    }
    public String getGenesisBalancesJSON(String url, int firstIndex, int lastIndex) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getGenesisBalances");
        params.put("firstIndex", String.valueOf(firstIndex));
        params.put("lastIndex", String.valueOf(lastIndex));
        return getJson(createURI(url), params);
    }
    public List<dto.Account> getGenesisBalances(String url) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getGenesisBalances");
        String json = getJson(createURI(url), params);
        JsonNode accountsNode = MAPPER.readTree(json).get("accounts");
        return MAPPER.readValue(accountsNode.toString(), new TypeReference<List<dto.Account>>(){});
    }

    public String startForging(String url, String secretPhrase) throws IOException {
        return sendForgingRequest(url, secretPhrase, "startForging", null);
    }

    private String sendForgingRequest(String url, String secretPhrase, String requestType, String adminPassword) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", requestType);
        if (secretPhrase != null) {
            parameters.put("secretPhrase", secretPhrase);
        }
        if (adminPassword != null) {
            parameters.put("adminPassword", adminPassword);
        }
        String json = postJson(createURI(url), parameters, "");
        return json;
    }

    public List<ForgingDetails> getForging(String url, String secretPhrase, String adminPassword) throws IOException {
        String json = sendForgingRequest(url, secretPhrase, "getForging", adminPassword);
        JsonNode root = MAPPER.readTree(json);
        JsonNode gereratorsArray = root.get("generators");
        return MAPPER.readValue(gereratorsArray.toString(), new TypeReference<List<ForgingDetails>>() {
        });
    }

    public String stopForging(String url, String secretPhrase) throws IOException {
        return sendForgingRequest(url, secretPhrase, "stopForging", null);
    }

    public NextGenerators getNextGenerators(String url, Long limit) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getNextBlockGenerators");
        parameters.put("limit", limit.toString());
        String json = getJson(createURI(url), parameters);
        return MAPPER.readValue(json, NextGenerators.class);
    }

    public JSONTransaction sendUpdateTransaction(String url, String secretPhrase, long feeATM, int level, DoubleByteArrayTuple updateUrl,
                                              Version version, Architecture architecture, Platform platform, String hash, int deadline) throws IOException, ParseException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "sendUpdateTransaction");
        parameters.put("version", version.toString());
        parameters.put("architecture", architecture.toString());
        parameters.put("platform", platform.toString());
        parameters.put("hash", hash);
        parameters.put("urlFirstPart", Convert.toHexString(updateUrl.getFirst()));
        parameters.put("urlSecondPart", Convert.toHexString(updateUrl.getSecond()));
        parameters.put("level", String.valueOf(level));
        return sendTransaction(url, secretPhrase, feeATM, deadline, parameters);
    }

    public Version getRemoteVersion(String url) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getState");
        String json = getJson(createURI(url), params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode versionString = root.get("version");
        return Version.from(versionString.asText());
    }

    public List<ChatInfo> getChatInfo(String url, String account, int firstIndex, int lastIndex) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getChats");
        params.put("account", account);
        putPagination(params, firstIndex, lastIndex);
        String json = getJson(createURI(url), params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode chatString = root.get("chats");
        return MAPPER.readValue(chatString.toString(), new TypeReference<List<ChatInfo>>() {});
    }

    public List<ChatInfo> getChatInfo(String url, String account) throws IOException {
        return getChatInfo(url, account, 0, -1);
    }

    public List<JSONTransaction> getChatHistory(String url, String account1, String account2, int firstIndex, int lastIndex) throws IOException {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getChatHistory");
        params.put("account1", account1);
        params.put("account2", account2);
        putPagination(params, firstIndex, lastIndex);
        String json = getJson(uri, params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("chatHistory");
        List<JSONTransaction> transactionsList = MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<JSONTransaction>>() {});
        return transactionsList;
    }

    public List<JSONTransaction> getAllTransactions(String url, int firstIndex, int lastIndex, byte type, byte subtype) throws IOException {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getAllTransactions");
        putPagination(params, firstIndex, lastIndex);
        String json = getJson(uri, params);
        JsonNode root = MAPPER.readTree(json);
        JsonNode transactionsArray = root.get("transactions");
        List<JSONTransaction> transactionsList = MAPPER.readValue(transactionsArray.toString(), new TypeReference<List<JSONTransaction>>() {});
        return transactionsList;
    }

    public AccountsStatistic getAccountsStatistic(String url, int numberOfAccounts) throws IOException {
        Map<String, String> params = new HashMap<>();
        URI uri = createURI(url);
        params.put("requestType", "getAccounts");
        params.put("numberOfAccounts", String.valueOf(numberOfAccounts));
        String json = getJson(uri, params);
        return MAPPER.readValue(json, AccountsStatistic.class);
    }

    public List<JSONTransaction> getAllTransactions(String url, int firstIndex, int lastIndex) throws IOException {
        return getAllTransactions(url, firstIndex, lastIndex, (byte) -1, (byte) -1);
    }

    public List<JSONTransaction> getAllTransactions(String url, byte type, byte subtype) throws IOException {
        return getAllTransactions(url, 0, -1, type, subtype);
    }

    public List<JSONTransaction> getAllTransactions(String url) throws IOException {
        return getAllTransactions(url, 0, -1, (byte) -1, (byte) -1);
    }

    public JSONTransaction sendTransaction(String url, String secretPhrase, long feeATM, long deadline, Map<String, String> specificParams) throws ParseException, IOException {
        Map<String, String> transactionParams = new HashMap<>();
        transactionParams.putAll(specificParams);
        transactionParams.put("secretPhrase", secretPhrase);
        transactionParams.put("feeATM", String.valueOf(feeATM));
        transactionParams.put("deadline", String.valueOf(deadline));
        String json = postJson(createURI(url), transactionParams, "");
        String transactionJSON = null;
        try {
            transactionJSON = ((JSONObject) ((JSONObject) PARSER.parse(json)).get("transactionJSON")).toJSONString();
        }
        catch (Exception e) {
            throw new RuntimeException("Unsuccessful request: " + json, e);
        }
        return MAPPER.readValue(transactionJSON, JSONTransaction.class);
    }

    public JSONTransaction sendChatTransaction(String url, String message, String secretPhrase, int deadline, String recipient, long feeATM) throws ParseException,
            IOException {
        Map<String, String> transactionParams = new HashMap<>();
        transactionParams.put("requestType", "sendMessage");
        transactionParams.put("message", message);
        transactionParams.put("recipient", recipient);
        return sendTransaction(url, secretPhrase, feeATM, deadline, transactionParams);
    }

    public JSONTransaction setAccountInfo(String url, String name, String secretPhrase, int deadline, long feeATM) throws ParseException,
            IOException {
        Map<String, String> transactionParams = new HashMap<>();
        transactionParams.put("requestType", "setAccountInfo");
        transactionParams.put("name", name);
        return sendTransaction(url, secretPhrase, feeATM, deadline, transactionParams);
    }

    private void putPagination(Map<String, String> params, int firstIndex, int lastIndex) {
        params.put("firstIndex", String.valueOf(firstIndex));
        params.put("lastIndex", String.valueOf(lastIndex));
    }

    public GeneratedAccount generateAccount(String url, String passphrase) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "generateAccount");
        if (passphrase != null && !passphrase.isEmpty()) {
            parameters.put("passphrase", passphrase);
        }
        String json = postJson(createURI(url), parameters, "");
        return MAPPER.readValue(json, GeneratedAccount.class);
    }

    public AccountWithKey exportKey(String url, String passphrase, String account) throws IOException {
        Objects.requireNonNull(passphrase);
        Objects.requireNonNull(account);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "exportKey");
        parameters.put("passphrase", passphrase);
        parameters.put("account", account);

        String json = postJson(createURI(url), parameters, "");
        AccountWithKey accountWithKey =  MAPPER.readValue(json, AccountWithKey.class);
        if (accountWithKey == null || accountWithKey.getId() == 0 || accountWithKey.getSecretBytes() == null) {
            throw new RuntimeException("Account is null. Bad json: " + json);
        }
        return accountWithKey;
    }

    public String importKey(String url, String passphrase, String secretBytes) throws IOException {
        String json = importKeyJson(url, passphrase, secretBytes);
        JsonNode jsonNode = MAPPER.readTree(json);
        JsonNode passphraseNode = jsonNode.get("passphrase");
        if (passphraseNode == null) {
            throw new RuntimeException("No passphrase in response: " + json);
        }
        return passphraseNode.textValue();
    }

    public String importKeyJson(String url, String passphrase, String secretBytes) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "importKey");
        if (passphrase != null && !passphrase.isEmpty()) {
            parameters.put("passphrase", passphrase);
        }
        parameters.put("secretBytes", secretBytes);

        String json = postJson(createURI(url), parameters, "");
        return json;
    }

    TwoFactorAuthAccountDetails enable2FA(String url, String account, String passphrase, String secretPhrase) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "enable2FA");
        if (secretPhrase == null) {
            parameters.put("passphrase", passphrase);
            parameters.put("account", account);
        } else {
            parameters.put("secretPhrase", secretPhrase);
        }

        String json = postJson(createURI(url), parameters, "");
        TwoFactorAuthAccountDetails details2FA = MAPPER.readValue(json, TwoFactorAuthAccountDetails.class);
        if (details2FA.getDetails().getSecret() == null) {
            throw new RuntimeException("No 2fa key in response");
        }
        return details2FA;
    }
    public TwoFactorAuthAccountDetails enable2FA(String url, String secretPhrase) throws IOException {
        return enable2FA(url, null, null, secretPhrase);
    }
    public TwoFactorAuthAccountDetails enable2FA(String url, String account, String passphrase) throws IOException {
        return enable2FA(url, account, passphrase, null);
    }

    public Account2FA disable2FA(String url, String account,String passphrase, String secretPhrase, long code) throws IOException {
        String json = disable2FAJson(url, account, passphrase, secretPhrase, code);
        return MAPPER.readValue(json, Account2FA.class);
    }
    String disable2FAJson(String url, String account,String passphrase, String secretPhrase, long code) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "disable2FA");
        if (secretPhrase == null) {

            parameters.put("passphrase", passphrase);
            parameters.put("account", account);
        } else {
            parameters.put("secretPhrase", secretPhrase);
        }
        parameters.put("code2FA", String.valueOf(code));

        return  postJson(createURI(url), parameters, "");
    }

    public String disable2FAJson(String url, String account, String passphrase, long code) throws IOException {
        return disable2FAJson(url, account, passphrase, null, code);
    }

    public String disable2FAJson(String url, String secretPhrase, long code) throws IOException {
        return disable2FAJson(url, null, null, secretPhrase, code);
    }

    Account2FA confirm2FA(String url, BasicAccount account, String passphrase, String secretPhrase, long code) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "confirm2FA");
        if (secretPhrase == null) {
            parameters.put("passphrase", passphrase);
            parameters.put("account", account.getAccountRS());
        } else {
            parameters.put("secretPhrase", secretPhrase);
        }
        parameters.put("code2FA", String.valueOf(code));

        String json = postJson(createURI(url), parameters, "");

        return MAPPER.readValue(json, Account2FA.class);
    }

    public Account2FA confirm2FA(String url, String secretPhrase, long code) throws IOException {
        return confirm2FA(url, null, null, secretPhrase, code);
    }

    public Account2FA confirm2FA(String url,BasicAccount account, String passphrase, long code) throws IOException {
        return confirm2FA(url, account, passphrase, null, code);
    }

    public String deleteKeyJson(String url, long accountId, String passphrase, long code) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "deleteKey");
        parameters.put("passphrase", passphrase);
        parameters.put("account", String.valueOf(accountId));
        if (code != 0) {
            parameters.put("code2FA", String.valueOf(code));
        }

        String json = postJson(createURI(url), parameters, "");
        return json;
    }

    public BasicAccount deleteKey(String url, long accountId, String passphrase, long code) throws IOException {
        String json = deleteKeyJson(url, accountId, passphrase, code);
        return MAPPER.readValue(json, BasicAccount.class);
    }

}
