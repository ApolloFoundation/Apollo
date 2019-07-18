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

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

@Slf4j
@Singleton
public final class GenesisImporter {

    private static byte[] CREATOR_PUBLIC_KEY;
    public static long CREATOR_ID;
    public static long EPOCH_BEGINNING;
    public static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    public static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";

    private BlockchainConfig blockchainConfig;
    private ConfigDirProvider configDirProvider;
    private AplAppStatus aplAppStatus;

    private BlockchainConfigUpdater blockchainConfigUpdater;
    private DatabaseManager databaseManager; // lazy init
    private String genesisTaskId;
    private JSONObject genesisAccountsJSON = null;

    @Inject
    public GenesisImporter(BlockchainConfig blockchainConfig, ConfigDirProvider configDirProvider,
                           BlockchainConfigUpdater blockchainConfigUpdater,
                           DatabaseManager databaseManager,
                           AplAppStatus aplAppStatus) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.configDirProvider = Objects.requireNonNull(configDirProvider, "configDirProvider is NULL");
        this.blockchainConfigUpdater = Objects.requireNonNull(blockchainConfigUpdater, "blockchainConfigUpdater is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        loadDataFromResources();
    }

    private void loadDataFromResources() {
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

/*
    private TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }
*/


    private byte[] loadGenesisAccountsJSON() {
        if (genesisTaskId == null) {
            Optional<DurableTaskInfo> task = aplAppStatus.findTaskByName("Shard data import");
            if (task.isPresent()) {
                genesisTaskId  = task.get().getId();
            } else {
                genesisTaskId = aplAppStatus.durableTaskStart("Genesis account load", "Loading and creating Genesis accounts + balances",true);
            }
        }

        MessageDigest digest = Crypto.sha256();
        String path = "conf/"+blockchainConfig.getChain().getGenesisLocation();
        log.trace("path = {}", path);
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

    public Block newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadGenesisAccountsJSON());
    }

    public void apply(boolean loadOnlyPublicKeys) {
        if (genesisAccountsJSON == null) {
            loadGenesisAccountsJSON();
        }
        blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.reset();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        // load 'public Keys' from JSON only
        if(!dataSource.isInTransaction()){
            dataSource.begin();
        }
        JSONArray publicKeys = loadPublicKeys(dataSource);
        dataSource.commit(false);
        if (loadOnlyPublicKeys) {
            log.debug("The rest of GENESIS is skipped, shard info will be loaded...");
            return;
        }
        // load 'balances' from JSON only
        long total = loadBalances(dataSource, publicKeys);

        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        String message = String.format("Total balance %f %s", (double)total / Constants.ONE_APL, blockchainConfig.getCoinSymbol());
        Account creatorAccount = Account.addOrGetAccount(GenesisImporter.CREATOR_ID, true);
        creatorAccount.apply(GenesisImporter.CREATOR_PUBLIC_KEY, true);
        creatorAccount.addToBalanceAndUnconfirmedBalanceATM(null, 0, -total);
        genesisAccountsJSON = null;
        aplAppStatus.durableTaskFinished(genesisTaskId, false, message);
        genesisTaskId = null;
    }

    private long loadBalances(TransactionalDataSource dataSource, JSONArray publicKeys) {
        int count;
        log.debug("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisAccountsJSON.get("balances");
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50+0.1, "Loading genesis balance amounts");
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
                log.debug(message);
                aplAppStatus.durableTaskUpdate(genesisTaskId, 50+(count*1.0/balances.size()*1.0)*50, message);
            }
        }
        return total;
    }

    private JSONArray loadPublicKeys(TransactionalDataSource dataSource) {
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisAccountsJSON.get("publicKeys");

        log.debug("Loading public keys [{}]...", publicKeys.size());
        aplAppStatus.durableTaskUpdate(genesisTaskId, 0.2, "Loading public keys");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = Account.addOrGetAccount(Account.getId(publicKey), true);
            account.apply(publicKey, true);
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (publicKeys.size() > 20000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_PUB_KEYS, count, publicKeys.size());
                aplAppStatus.durableTaskUpdate(genesisTaskId, (count*1.0/publicKeys.size()*1.0)*50, message);
            }
        }
        return publicKeys;
    }

    public List<Map.Entry<String, Long>> loadGenesisAccounts() {

        // Original line below:
        String path = configDirProvider.getConfigDirectoryName()+"/"+blockchainConfig.getChain().getGenesisLocation();
        // Hotfixed because UNIX way working everywhere
        // TODO: Fix that for crossplatform compatibility

//            String path = aplCoreRuntime.getConfDir()+"/"+blockchainConfig.getChain().getGenesisLocation();

        log.debug("Genesis accounts path = " + path);
        try (InputStreamReader is = new InputStreamReader(
                GenesisImporter.class.getClassLoader().getResourceAsStream(path))) {
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

}
