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

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Constants;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

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
import javax.enterprise.inject.spi.CDI;

public final class Genesis {
    private static final Logger LOG = getLogger(Genesis.class);

    private static final byte[] CREATOR_PUBLIC_KEY;
    public static final long CREATOR_ID;
    public static final long EPOCH_BEGINNING;
    public static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    public static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";

    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    private static BlockchainConfigUpdater blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
    private static DatabaseManager databaseManager; // lazy init

    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("conf/data/genesisParameters.json")) {
            JSONObject genesisParameters = (JSONObject)JSONValue.parseWithException(new InputStreamReader(is));
            CREATOR_PUBLIC_KEY = Convert.parseHexString((String)genesisParameters.get("genesisPublicKey"));
            CREATOR_ID = Account.getId(CREATOR_PUBLIC_KEY);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            EPOCH_BEGINNING = dateFormat.parse((String) genesisParameters.get("epochBeginning")).getTime();
        } catch (ParseException|IOException|java.text.ParseException e) {
            throw new RuntimeException("Failed to load genesis parameters", e);
        }
        
    }

    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    private static JSONObject genesisAccountsJSON = null;

    private static byte[] loadGenesisAccountsJSON() {
        MessageDigest digest = Crypto.sha256();
        String path = "conf/"+blockchainConfig.getChain().getGenesisLocation();
        try (InputStreamReader is = new InputStreamReader(new DigestInputStream(
                ClassLoader.getSystemResourceAsStream(path), digest))) {
            genesisAccountsJSON = (JSONObject) JSONValue.parseWithException(is);
        } catch (ParseException|IOException e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }
        // we should leave here '0' to create correct genesis block for already launched mainnet
        digest.update((byte)(0));
        digest.update(Convert.toBytes(EPOCH_BEGINNING));
        return digest.digest();
    }

    static Block newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadGenesisAccountsJSON());
    }

    static void apply() {
        if (genesisAccountsJSON == null) {
            loadGenesisAccountsJSON();
        }

        blockchainConfigUpdater.reset();
        TransactionalDataSource dataSource = lookupDataSource();
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisAccountsJSON.get("publicKeys");
        String loadingPublicKeysString = "Loading public keys";
        LOG.debug("Loading public keys [{}]...", publicKeys.size());
        AppStatus.getInstance().update(loadingPublicKeysString + "...");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = Account.addOrGetAccount(Account.getId(publicKey), true);
            account.apply(publicKey, true);
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (publicKeys.size() > 20000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_PUB_KEYS, count, publicKeys.size());
                LOG.debug(message);
                AppStatus.getInstance().update(message);
            }
        }
        LOG.debug("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisAccountsJSON.get("balances");
        String loadingAmountsString = "Loading genesis amounts";
        LOG.debug(loadingAmountsString);
        AppStatus.getInstance().update(loadingAmountsString + "...");
        long total = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            Account account = Account.addOrGetAccount(Long.parseUnsignedLong(entry.getKey()), true);
            account.addToBalanceAndUnconfirmedBalanceATM(null, 0, entry.getValue());
            total += entry.getValue();
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (balances.size() > 10000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_GENESIS_BALANCE, count, balances.size());
                LOG.debug(message);
                AppStatus.getInstance().update(message);
            }
        }
        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        LOG.debug(String.format("Total balance %f %s", (double)total / Constants.ONE_APL, blockchainConfig.getCoinSymbol()));
        Account creatorAccount = Account.addOrGetAccount(Genesis.CREATOR_ID, true);
        creatorAccount.apply(Genesis.CREATOR_PUBLIC_KEY, true);
        creatorAccount.addToBalanceAndUnconfirmedBalanceATM(null, 0, -total);
        genesisAccountsJSON = null;
    }

        public static List<Map.Entry<String, Long>> loadGenesisAccounts() {
            String path = "conf/"+blockchainConfig.getChain().getGenesisLocation();
            try (InputStreamReader is = new InputStreamReader(
                    Genesis.class.getClassLoader().getResourceAsStream(path))) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(is);
                JsonNode balancesArray = root.get("balances");
                Map<String, Long> map = objectMapper.readValue(balancesArray.toString(), new TypeReference<Map<String, Long>>(){});

                return map.entrySet()
                        .stream()
                        .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                        .skip(1) //skip first account to collect only genesis accounts
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load genesis accounts", e);
            }
        }

    private Genesis() {} // never

}
