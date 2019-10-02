/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
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

@Slf4j
@Singleton
public class GenesisImporter {

    private byte[] CREATOR_PUBLIC_KEY;
    public static long CREATOR_ID;
    public static long EPOCH_BEGINNING;
    public static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    public static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";

    private BlockchainConfig blockchainConfig;
    private ConfigDirProvider configDirProvider;
    private AplAppStatus aplAppStatus;

    private BlockchainConfigUpdater blockchainConfigUpdater;
    private DatabaseManager databaseManager;
    private String genesisTaskId;
    private JsonNode genesisAccountsJSON = null;
    private List<String> publicKeys;
    private Map<String, Long> balances;
    private byte[] computedDigest;
    private ObjectMapper mapper = new ObjectMapper();
    private String genesisParametersLocation;

    @Inject
    public GenesisImporter(BlockchainConfig blockchainConfig, ConfigDirProvider configDirProvider,
                           BlockchainConfigUpdater blockchainConfigUpdater,
                           DatabaseManager databaseManager,
                           AplAppStatus aplAppStatus,
                           GenesisImporterProducer genesisImporterProducer) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.configDirProvider = Objects.requireNonNull(configDirProvider, "configDirProvider is NULL");
        this.blockchainConfigUpdater = Objects.requireNonNull(blockchainConfigUpdater, "blockchainConfigUpdater is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        Objects.requireNonNull(genesisImporterProducer);
        this.genesisParametersLocation = genesisImporterProducer.genesisParametersLocation();
    }

    @PostConstruct
    public void loadGenesisDataFromResources() {
        if (CREATOR_PUBLIC_KEY == null) {
            try (InputStream is = ClassLoader.getSystemResourceAsStream(genesisParametersLocation)) {
                JsonNode genesisParameters = mapper.readTree(is);
                CREATOR_PUBLIC_KEY = Convert.parseHexString(genesisParameters.get("genesisPublicKey").asText());
                CREATOR_ID = Account.getId(CREATOR_PUBLIC_KEY);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                EPOCH_BEGINNING = dateFormat.parse(genesisParameters.get("epochBeginning").asText()).getTime();
             } catch (IOException | java.text.ParseException e) {
                log.error("genesis Parameters were not loaded = {}", e.getMessage());
                throw new RuntimeException("Failed to load genesis parameters", e);
            }
        }

    }

    private byte[] loadBalancesAccountsComputeDigest() {
        long start = System.currentTimeMillis();
        if (genesisTaskId == null) {
            Optional<DurableTaskInfo> task = aplAppStatus.findTaskByName("Shard data import");
            if (task.isPresent()) {
                genesisTaskId  = task.get().getId();
            } else {
                genesisTaskId = aplAppStatus.durableTaskStart("Genesis account load", "Loading and creating Genesis accounts + balances",true);
            }
        }

        MessageDigest digest = Crypto.sha256();
        String path = blockchainConfig.getChain().getGenesisLocation();
        log.trace("path = {}", path);
        try (InputStreamReader is = new InputStreamReader(new DigestInputStream(
                ClassLoader.getSystemResourceAsStream(path), digest))) {
            genesisAccountsJSON = mapper.readTree(is);
            traceDumpData("genesisAccountsJSON = {}", genesisAccountsJSON);
            JsonNode balances = genesisAccountsJSON.get("balances");
            this.balances = mapper.readValue(balances.toString(), new TypeReference<Map<String, Long>>(){});
            log.debug("balances = [{}]", this.balances.size());
            traceDumpData("balances = {}", this.balances);
            JsonNode publicKeys = genesisAccountsJSON.get("publicKeys");
            this.publicKeys = mapper.readValue(publicKeys.toString(), new TypeReference<List<String>>(){});
            log.debug("publicKeys = [{}]", this.publicKeys.size());
            traceDumpData("publicKeys = {}", this.publicKeys);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }
        // we should leave here '0' to create correct genesis block for already launched mainnet
        digest.update((byte)(0));
        digest.update(Convert.toBytes(EPOCH_BEGINNING));
        this.computedDigest = digest.digest();
        genesisAccountsJSON = null;
        Long usedBytes = null; // Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(); // to measure in unit tests
        log.debug("Digest is computed in {} milliSec, used {} Kb", System.currentTimeMillis() - start,
                usedBytes != null ? usedBytes / 1024 : "not calculated");
        return this.computedDigest;
    }

    private void traceDumpData(String pattern, Object... data) {
        if (log.isTraceEnabled()) {
            log.trace(pattern, data);
        }
    }

    public Block newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadBalancesAccountsComputeDigest());
    }

    @Transactional
    public void importGenesisJson(boolean loadOnlyPublicKeys) {
        long start = System.currentTimeMillis();
        if (this.balances == null || this.publicKeys == null) {
            loadBalancesAccountsComputeDigest();
        }
        blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        blockchainConfigUpdater.reset();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        // load 'public Keys' from JSON only
        if(!dataSource.isInTransaction()){
            dataSource.begin();
        }
        savePublicKeys(dataSource);
        dataSource.commit(false);
        if (loadOnlyPublicKeys) {
            this.publicKeys = null;
            this.balances = null;
            log.debug("Public Keys were saved in {} ms. The rest of GENESIS is skipped, shard info will be loaded...",
                    (System.currentTimeMillis() - start) / 1000);
            return;
        }
        // load 'balances' from JSON only
        long total = saveBalances(dataSource);

        long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        String message = String.format("Total balance %f %s", (double)total / Constants.ONE_APL, blockchainConfig.getCoinSymbol());
        Account creatorAccount = Account.addOrGetAccount(CREATOR_ID, true);
        creatorAccount.apply(CREATOR_PUBLIC_KEY, true);
        creatorAccount.addToBalanceAndUnconfirmedBalanceATM(null, 0, -total);
        genesisAccountsJSON = null;
        aplAppStatus.durableTaskFinished(genesisTaskId, false, message);
        log.debug("Public Keys [{}] + Balances [{}] were saved in {} ms", this.publicKeys.size(), this.balances.size(),
                (System.currentTimeMillis() - start) / 1000);
        genesisTaskId = null;
        this.publicKeys = null;
        this.balances = null;
    }

    private void savePublicKeys(TransactionalDataSource dataSource) {
        long start = System.currentTimeMillis();
        int count = 0;
        log.trace("Saving public keys [{}]...", this.publicKeys.size());
        aplAppStatus.durableTaskUpdate(genesisTaskId, 0.2, "Loading public keys");
        for (Object jsonPublicKey : this.publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            long id = Account.getId(publicKey);
            log.trace("AccountId = '{}' by publicKey string = '{}'", id, jsonPublicKey);
            Account account = Account.addOrGetAccount(id, true);
            account.apply(publicKey, true);
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (count % 10000 == 0) {
                String message = String.format(LOADING_STRING_PUB_KEYS, count, this.publicKeys.size());
                log.debug(message);
                aplAppStatus.durableTaskUpdate(genesisTaskId, (count*1.0/this.publicKeys.size()*1.0)*50, message);
            }
        }
        log.debug("Saved public keys = [{}] in {} sec", this.publicKeys.size(), (System.currentTimeMillis() - start) / 1000);
    }

    private long saveBalances(TransactionalDataSource dataSource) {
        long start = System.currentTimeMillis();
        int count;
        log.trace("Saved [{}] public keys, start saving Balances...", this.publicKeys.size());
        count = 0;
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50+0.1, "Loading genesis balance amounts");
        long totalAmount = 0;
        for (Map.Entry<String, Long> entry : ((Map<String, Long>)this.balances).entrySet()) {
            log.trace("Parsed json balance entry: {} - {}", entry.getKey(), entry.getValue());
            Account account = Account.addOrGetAccount(Long.parseUnsignedLong(entry.getKey()), true);
            account.addToBalanceAndUnconfirmedBalanceATM(null, 0, entry.getValue());
            totalAmount += entry.getValue();
            if (count++ % 100 == 0) {
                dataSource.commit(false);
            }
            if (count % 10000 == 0) {
                String message = String.format(LOADING_STRING_GENESIS_BALANCE, count, this.balances.size());
                log.debug(message);
                aplAppStatus.durableTaskUpdate(genesisTaskId, 50+(count*1.0/this.balances.size()*1.0)*50, message);
            }
        }
        log.debug("Saved [{}] balances in {} sec, total balance amount = {}", this.balances.size(),
                (System.currentTimeMillis() - start) / 1000, totalAmount);
        return totalAmount;
    }

    public List<Map.Entry<String, Long>> loadGenesisAccounts() {
        String path = blockchainConfig.getChain().getGenesisLocation();
        log.debug("Genesis accounts json resource path = " + path);
        try (InputStreamReader is = new InputStreamReader(
                Objects.requireNonNull(
                        GenesisImporter.class.getClassLoader().getResourceAsStream(path),
                        "Genesis accounts json was NOT found as resource by path = " + path))) {
            JsonNode root = mapper.readTree(is);
            JsonNode balancesArray = root.get("balances");
            Map<String, Long> map = mapper.readValue(balancesArray.toString(), new TypeReference<Map<String, Long>>(){});

            return map.entrySet()
                    .stream()
                    .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                    .skip(1) //skip first account to collect only genesis accounts
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load genesis accounts", e);
        }
    }

    public byte[] getCreatorPublicKey() {
        return CREATOR_PUBLIC_KEY;
    }

    public byte[] getComputedDigest() {
        return computedDigest;
    }

    public List<String> getPublicKeys() {
        return publicKeys;
    }

    public Map<String, Long> getBalances() {
        return balances;
    }

}
