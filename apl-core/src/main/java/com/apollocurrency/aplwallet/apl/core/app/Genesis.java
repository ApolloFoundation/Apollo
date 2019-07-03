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

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
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
    public static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    public static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";

    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static  ConfigDirProvider configDirProvider = CDI.current().select(ConfigDirProvider.class).get();
//    private static AplCoreRuntime aplCoreRuntime  = CDI.current().select(AplCoreRuntime.class).get();
    private static AplAppStatus aplAppStatus = CDI.current().select(AplAppStatus.class).get();;
    private static AccountService accountService = CDI.current().select(AccountServiceImpl .class).get();
    private static AccountPublicKeyService accountPublicKeyService;// = CDI.current().select(AccountPublicKeyServiceImpl.class).get();
    private static BlockchainConfigUpdater blockchainConfigUpdater;// = CDI.current().select(BlockchainConfigUpdater.class).get();
    private static DatabaseManager databaseManager; // lazy init
    private static String genesisTaskId;
    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("conf/data/genesisParameters.json")) {
            JSONObject genesisParameters = (JSONObject)JSONValue.parseWithException(new InputStreamReader(is));
            CREATOR_PUBLIC_KEY = Convert.parseHexString((String)genesisParameters.get("genesisPublicKey"));
            CREATOR_ID = AccountService.getId(CREATOR_PUBLIC_KEY);
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

    private static AccountPublicKeyService lookupAccountPublicKeyService() {
        if ( accountPublicKeyService == null ) {
            accountPublicKeyService = CDI.current().select(AccountPublicKeyServiceImpl.class).get();
        }
        return accountPublicKeyService;
    }

    private static JSONObject genesisAccountsJSON = null;

    private static byte[] loadGenesisAccountsJSON() {
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

    public static Block newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadGenesisAccountsJSON());
    }

    static void apply(boolean loadOnlyPublicKeys) {
        if (genesisAccountsJSON == null) {
            loadGenesisAccountsJSON();
        }
        blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.reset();
        TransactionalDataSource dataSource = lookupDataSource();
        // load 'public Keys' from JSON only
        if(!dataSource.isInTransaction()){
            dataSource.begin();
        }
        JSONArray publicKeys = loadPublicKeys(dataSource);
        dataSource.commit(false);
        if (loadOnlyPublicKeys) {
            LOG.debug("The rest of GENESIS is skipped, shard info will be loaded...");
            return;
        }
        // load 'balances' from JSON only
        long total = loadBalances(dataSource, publicKeys);

        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        String message = String.format("Total balance %f %s", (double)total / Constants.ONE_APL, blockchainConfig.getCoinSymbol());
        Account creatorAccount = accountService.addOrGetAccount(Genesis.CREATOR_ID, true);
        lookupAccountPublicKeyService().apply(creatorAccount, Genesis.CREATOR_PUBLIC_KEY, true);
        accountService.addToBalanceAndUnconfirmedBalanceATM(creatorAccount, null, 0, -total);
        genesisAccountsJSON = null;
        aplAppStatus.durableTaskFinished(genesisTaskId, false, message);
        genesisTaskId = null;
    }

    private static long loadBalances(TransactionalDataSource dataSource, JSONArray publicKeys) {
        int count;
        LOG.debug("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisAccountsJSON.get("balances");
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50+0.1, "Loading genesis balance amounts");
        long total = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)balances).entrySet()) {
            Account account = accountService.addOrGetAccount(Long.parseUnsignedLong(entry.getKey()), true);
            accountService.addToBalanceAndUnconfirmedBalanceATM(account, null, 0, entry.getValue());
            total += entry.getValue();
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (balances.size() > 10000 && count % 10000 == 0) {
                String message = String.format(LOADING_STRING_GENESIS_BALANCE, count, balances.size());
                LOG.debug(message);
                aplAppStatus.durableTaskUpdate(genesisTaskId, 50+(count*1.0/balances.size()*1.0)*50, message);
            }
        }
        return total;
    }

    private static JSONArray loadPublicKeys(TransactionalDataSource dataSource) {
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisAccountsJSON.get("publicKeys");

        LOG.debug("Loading public keys [{}]...", publicKeys.size());
        aplAppStatus.durableTaskUpdate(genesisTaskId, 0.2, "Loading public keys");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = accountService.addOrGetAccount(AccountService.getId(publicKey), true);
            lookupAccountPublicKeyService().apply(account, publicKey, true);
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

    public static List<Map.Entry<String, Long>> loadGenesisAccounts() {
            
            // Original line below:
             String path = configDirProvider.getConfigDirectoryName()+"/"+blockchainConfig.getChain().getGenesisLocation();
            // Hotfixed because UNIX way working everywhere
            // TODO: Fix that for crossplatform compatibility

//            String path = aplCoreRuntime.getConfDir()+"/"+blockchainConfig.getChain().getGenesisLocation();

            LOG.debug("Genesis accounts path = " + path);
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
