/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.mint;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.util.Constants;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.TrustAllSSLProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import static org.slf4j.LoggerFactory.getLogger;

public class MintWorker implements Runnable{
    private boolean done = false;
    
    private static final Logger LOG = getLogger(MintWorker.class);
    // TODO: YL remove static instance later
    private PropertiesHolder propertiesHolder;
    private BlockchainConfig blockchainConfig;

    public MintWorker(PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig) {
        this.blockchainConfig=blockchainConfig;
        this.propertiesHolder=propertiesHolder;
    }
    
    public void stop(){
        done = true;
    }

    public void run() {
        String currencyCode = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.mint.currencyCode"));
        if (currencyCode == null) {
            throw new IllegalArgumentException("apl.mint.currencyCode not specified");
        }
        byte[] keySeed = null;
        String secretPhrase = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.mint.secretPhrase", null, true));
        if (secretPhrase == null) {
            String ks = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.mint.keySeed", null, true));
            if (ks == null) {
                throw new IllegalArgumentException("either apl.mint.secretPhrase or apl.mint.keySeed should be specified");
            } else {
                keySeed = Convert.parseHexString(ks);
            }
        } else {
            keySeed = Crypto.getKeySeed(secretPhrase);
        }


        boolean isSubmitted = propertiesHolder.getBooleanProperty("apl.mint.isSubmitted");
        boolean isStopOnError = propertiesHolder.getBooleanProperty("apl.mint.stopOnError");
        byte[] publicKeyHash = Crypto.sha256().digest(Crypto.getPublicKey(secretPhrase));
        long accountId = Convert.fullHashToId(publicKeyHash);
        String rsAccount = Convert2.rsAccount(accountId);
        JSONObject currency = getCurrency(currencyCode);
        if (currency.get("currency") == null) {
            throw new IllegalArgumentException("Invalid currency code " + currencyCode);
        }
        long currencyId = Convert.parseUnsignedLong((String) currency.get("currency"));
        if (currency.get("algorithm") == null) {
            throw new IllegalArgumentException("Minting algorithm not specified, currency " + currencyCode + " is not mintable");
        }
        byte algorithm = (byte)(long) currency.get("algorithm");
        byte decimal = (byte)(long) currency.get("decimals");
        String unitsStr = propertiesHolder.getStringProperty("apl.mint.unitsPerMint");
        double wholeUnits = 1;
        if (unitsStr != null && unitsStr.length() > 0) {
            wholeUnits = Double.parseDouble(unitsStr);
        }
        long units = (long)(wholeUnits * Math.pow(10, decimal));
        JSONObject mintingTarget = getMintingTarget(currencyId, rsAccount, units);
        long counter = (long) mintingTarget.get("counter");
        byte[] target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
        BigInteger difficulty = new BigInteger((String)mintingTarget.get("difficulty"));
        long initialNonce = propertiesHolder.getIntProperty("apl.mint.initialNonce");
        if (initialNonce == 0) {
            initialNonce = new Random().nextLong();
        }
        int threadPoolSize = propertiesHolder.getIntProperty("apl.mint.threadPoolSize");
        if (threadPoolSize == 0) {
            threadPoolSize = Runtime.getRuntime().availableProcessors();
            LOG.debug("Thread pool size " + threadPoolSize);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        LOG.info("Mint worker started");
        while (done) {
            counter++;
            try {
                JSONObject response = mintImpl(keySeed, accountId, units, currencyId, algorithm, counter, target,
                    initialNonce, threadPoolSize, executorService, difficulty, isSubmitted);
                LOG.info("currency mint response:" + response.toJSONString());
            } catch (Exception e) {
                LOG.info("mint error", e);
                if (isStopOnError) {
                    LOG.info("stopping on error");
                    break;
                } else {
                    LOG.info("continue");
                }
            }
            mintingTarget = getMintingTarget(currencyId, rsAccount, units);
            target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
            difficulty = new BigInteger((String)mintingTarget.get("difficulty"));
        }
    }

    private JSONObject mintImpl(byte[] keySeed, long accountId, long units, long currencyId, byte algorithm,
                                long counter, byte[] target, long initialNonce, int threadPoolSize, ExecutorService executorService, BigInteger difficulty, boolean isSubmitted) {
        long startTime = System.currentTimeMillis();
        List<Callable<Long>> workersList = new ArrayList<>();
        for (int i=0; i < threadPoolSize; i++) {
            HashSolver hashSolver = new HashSolver(algorithm, currencyId, accountId, counter, units, initialNonce + i, target, threadPoolSize);
            workersList.add(hashSolver);
        }
        long solution = solve(executorService, workersList);
        long computationTime = System.currentTimeMillis() - startTime;
        if (computationTime == 0) {
            computationTime = 1;
        }
        long hashes = solution - initialNonce;
        float hashesPerDifficulty = BigInteger.valueOf(-1).equals(difficulty) ? 0 : (float) hashes / difficulty.floatValue();
        LOG.info("solution nonce %d unitsATM %d counter %d computed hashes %d time [sec] %.2f hash rate [KH/Sec] %d actual time vs. expected %.2f is submitted %b",
                solution, units, counter, hashes, (float) computationTime / 1000, hashes / computationTime, hashesPerDifficulty, isSubmitted);
        JSONObject response;
        if (isSubmitted) {
            response = currencyMint(keySeed, currencyId, solution, units, counter);
        } else {
            response = new JSONObject();
            response.put("message", "apl.mint.isSubmitted=false therefore currency mint transaction is not submitted");
        }
        return response;
    }

    private long solve(Executor executor, Collection<Callable<Long>> solvers) {
        CompletionService<Long> ecs = new ExecutorCompletionService<>(executor);
        List<Future<Long>> futures = new ArrayList<>(solvers.size());
        solvers.forEach(solver -> futures.add(ecs.submit(solver)));
        try {
            return ecs.take().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            for (Future<Long> f : futures) {
                f.cancel(true);
            }
        }
    }

    private JSONObject currencyMint(byte[] keySeed, long currencyId, long nonce, long units, long counter) {
        JSONObject ecBlock = getECBlock();
        Attachment attachment = new MonetarySystemCurrencyMinting(nonce, currencyId, units, counter);
        int timestamp = ((Long) ecBlock.get("timestamp")).intValue();
        Transaction.Builder builder = Transaction.newTransactionBuilder(Crypto.getPublicKey(keySeed), 0, Constants.ONE_APL,
                (short) 120, attachment, timestamp)
                .ecBlockHeight(((Long) ecBlock.get("ecBlockHeight")).intValue())
                .ecBlockId(Convert.parseUnsignedLong((String) ecBlock.get("ecBlockId")));
        try {
            Transaction transaction = builder.build(keySeed);
            Map<String, String> params = new HashMap<>();
            params.put("requestType", "broadcastTransaction");
            params.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            return getJsonResponse(params);
        } catch (AplException.NotValidException e) {
            LOG.info("local signing failed", e);
            JSONObject response = new JSONObject();
            response.put("error", e.toString());
            return response;
        }
    }

    private JSONObject getCurrency(String currencyCode) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getCurrency");
        params.put("code", currencyCode);
        return getJsonResponse(params);
    }

    private JSONObject getMintingTarget(long currencyId, String rsAccount, long units) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getMintingTarget");
        params.put("currency", Long.toUnsignedString(currencyId));
        params.put("account", rsAccount);
        params.put("units", Long.toString(units));
        return getJsonResponse(params);
    }

    private JSONObject getECBlock() {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getECBlock");
        return getJsonResponse(params);
    }

    private JSONObject getJsonResponse(Map<String, String> params) {
        JSONObject response;
        HttpURLConnection connection = null;
        String host = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.mint.serverAddress"));
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                host = "localhost";
            }
        }
        String protocol = "http";
        boolean useHttps = propertiesHolder.getBooleanProperty("apl.mint.useHttps");
        if (useHttps) {
            protocol = "https";
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        int port = propertiesHolder.getIntProperty("apl.apiServerPort");
        String urlParams = getUrlParams(params);
        URL url;
        try {
            url = new URL(protocol, host, port, "/apl?" + urlParams);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        try {
            LOG.debug("Sending request to server: " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    response = (JSONObject) JSONValue.parse(reader);
                }
            } else {
                response = null;
            }
        } catch (RuntimeException | IOException e) {
            LOG.info("Error connecting to server", e);
            if (connection != null) {
                connection.disconnect();
            }
            throw new IllegalStateException(e);
        }
        if (response == null) {
            throw new IllegalStateException(String.format("Request %s response error", url));
        }
        if (response.get("errorCode") != null) {
            throw new IllegalStateException(String.format("Request %s produced error response code %s message \"%s\"",
                    url, response.get("errorCode"), response.get("errorDescription")));
        }
        if (response.get("error") != null) {
            throw new IllegalStateException(String.format("Request %s produced error %s",
                    url, response.get("error")));
        }
        return response;
    }

    private static String getUrlParams(Map<String, String> params) {
        if (params == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            try {
                sb.append(key).append("=").append(URLEncoder.encode(params.get(key), "utf8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        String rc = sb.toString();
        if (rc.endsWith("&")) {
            rc = rc.substring(0, rc.length() - 1);
        }
        return rc;
    }

    private static class HashSolver implements Callable<Long> {

        private final HashFunction hashFunction;
        private final long currencyId;
        private final long accountId;
        private final long counter;
        private final long units;
        private final long nonce;
        private final byte[] target;
        private final int poolSize;

        private HashSolver(byte algorithm, long currencyId, long accountId, long counter, long units, long nonce,
                           byte[] target, int poolSize) {
            this.hashFunction = HashFunction.getHashFunction(algorithm);
            this.currencyId = currencyId;
            this.accountId = accountId;
            this.counter = counter;
            this.units = units;
            this.nonce = nonce;
            this.target = target;
            this.poolSize = poolSize;
        }

        @Override
        public Long call() {
            long n = nonce;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] hash = CurrencyMinting.getHash(hashFunction, n, currencyId, units, counter, accountId);
                if (CurrencyMinting.meetsTarget(hash, target)) {
                    LOG.debug("%s found solution hash %s nonce %d currencyId %d units %d counter %d accountId %d" +
                            " hash %s meets target %s",
                            Thread.currentThread().getName(), hashFunction, n, currencyId, units, counter, accountId,
                            Arrays.toString(hash), Arrays.toString(target));
                    return n;
                }
                n+=poolSize;
                if (((n - nonce) % (poolSize * 1000000)) == 0) {
                    LOG.info("%s computed %d [MH]", Thread.currentThread().getName(), (n - nonce) / poolSize / 1000000);
                }
            }
            return null;
        }
    }
}
