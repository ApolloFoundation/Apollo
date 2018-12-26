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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

public final class Genesis {
    private static final Logger LOG = getLogger(Genesis.class);

    private static final byte[] CREATOR_PUBLIC_KEY;
    public static final long CREATOR_ID;
    public static final long EPOCH_BEGINNING;
    private static final String GENESIS_ACCOUNTS_JSON_PATH = "data/genesisAccounts";
    private static final String GENESIS_ACCOUNTS_JSON_PATH_TESTNET_SUFFIX = "-testnet.json";
    private static final String GENESIS_ACCOUNTS_JSON_PATH_MAINNET_SUFFIX = ".json";

    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("data/genesisParameters.json")) {
            JSONObject genesisParameters = (JSONObject)JSONValue.parseWithException(new InputStreamReader(is));
            CREATOR_PUBLIC_KEY = Convert.parseHexString((String)genesisParameters.get("genesisPublicKey"));
            CREATOR_ID = Account.getId(CREATOR_PUBLIC_KEY);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            EPOCH_BEGINNING = dateFormat.parse((String) genesisParameters.get("epochBeginning")).getTime();
        } catch (IOException|ParseException|java.text.ParseException e) {
            throw new RuntimeException("Failed to load genesis parameters", e);
        }
    }

    private static JSONObject genesisAccountsJSON = null;

    private static byte[] loadGenesisAccountsJSON() {
        MessageDigest digest = Crypto.sha256();
        try (InputStreamReader is = new InputStreamReader(new DigestInputStream(
                ClassLoader.getSystemResourceAsStream(AplGlobalObjects.getChainConfig().getChain().getGenesisLocation()), digest))) {
            genesisAccountsJSON = (JSONObject) JSONValue.parseWithException(is);
        } catch (IOException|ParseException e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }
        digest.update((byte)(AplGlobalObjects.getChainConfig().isTestnet() ? 1 : 0));
        digest.update(Convert.toBytes(EPOCH_BEGINNING));
        return digest.digest();
    }

    static BlockImpl newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadGenesisAccountsJSON());
    }

    static void apply() {
        if (genesisAccountsJSON == null) {
            loadGenesisAccountsJSON();
        }
        AplGlobalObjects.getChainConfig().reset();
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisAccountsJSON.get("publicKeys");
        String loadingPublicKeysString = "Loading public keys";
        LOG.debug(loadingPublicKeysString);
        Apl.getRuntimeMode().updateAppStatus(loadingPublicKeysString + "...");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = Account.addOrGetAccount(Account.getId(publicKey), true);
            account.apply(publicKey, true);
            if (count++ % 100 == 0) {
                Db.getDb().commitTransaction();
            }
        }
        LOG.debug("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisAccountsJSON.get("balances");
        String loadingAmountsString = "Loading genesis amounts";
        LOG.debug(loadingAmountsString);
        Apl.getRuntimeMode().updateAppStatus(loadingAmountsString + "...");
        long total = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            Account account = Account.addOrGetAccount(Long.parseUnsignedLong(entry.getKey()), true);
            account.addToBalanceAndUnconfirmedBalanceATM(null, 0, entry.getValue());
            total += entry.getValue();
            if (count++ % 100 == 0) {
                Db.getDb().commitTransaction();
            }
        }
        long maxBalanceATM = AplGlobalObjects.getChainConfig().getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        LOG.debug("Total balance %f %s", (double)total / Constants.ONE_APL, AplGlobalObjects.getChainConfig().getCoinSymbol());
        Account creatorAccount = Account.addOrGetAccount(Genesis.CREATOR_ID, true);
        creatorAccount.apply(Genesis.CREATOR_PUBLIC_KEY, true);
        creatorAccount.addToBalanceAndUnconfirmedBalanceATM(null, 0, -total);
        genesisAccountsJSON = null;
    }

        public static List<Map.Entry<String, Long>> loadGenesisAccounts() {
            try (InputStreamReader is = new InputStreamReader(
                    Genesis.class.getClassLoader().getResourceAsStream(AplGlobalObjects.getChainConfig().getChain().getGenesisLocation()))) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(is);
                JsonNode balancesArray = root.get("balances");
                Map<String, Long> map = objectMapper.readValue(balancesArray.toString(), new TypeReference<Map<String, Long>>(){});

                return map.entrySet()
                        .stream()
                        .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                        .skip(1)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load genesis accounts", e);
            }
        }

    private Genesis() {} // never

}
