/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * @author al
 */
@Singleton
public class GenesisAccounts {
    private volatile List<Map.Entry<String, Long>> initialGenesisAccountsBalances;
    private  GenesisImporter genesisImporter;


    public void init() {
        if (genesisImporter == null) {
            genesisImporter = CDI.current().select(GenesisImporter.class).get();
        }

        genesisImporter.loadGenesisDataFromResources();

        try {
            initialGenesisAccountsBalances = genesisImporter.loadGenesisAccounts();
        } catch (GenesisImportException e) {
            throw new IllegalStateException("Unable load genesis accounts into memory", e);
        }
    }

    public List<Map.Entry<String, Long>> getGenesisBalances(int firstIndex, int lastIndex) {
        firstIndex = Math.max(firstIndex, 0);
        lastIndex = Math.max(lastIndex, 0);
        if (lastIndex < firstIndex) {
            throw new IllegalArgumentException("firstIndex should be less or equal lastIndex ");
        }
        if (firstIndex >= initialGenesisAccountsBalances.size() - 1 || lastIndex > initialGenesisAccountsBalances.size() - 1) {
            throw new IllegalArgumentException("firstIndex and lastIndex should be less than " + (initialGenesisAccountsBalances.size() - 1));
        }
        if (lastIndex - firstIndex > 99) {
            lastIndex = firstIndex + 99;
        }
        return initialGenesisAccountsBalances.subList(firstIndex + 1, lastIndex + 2);
    }

    /**
     * Return the original account balance from the genesis, required for the effective balance calculation, when shard
     * was imported with height < 1440
     * @param accountId id of the account to retrieve genesis balance
     * @return genesis balance of the given account or 0 if not found
     */
    public long getGenesisBalance(long accountId) {
        return initialGenesisAccountsBalances.stream()
            .filter(e -> e.getKey().equals(Long.toUnsignedString(accountId)))
            .map(Map.Entry::getValue)
            .findAny()
            .orElse(0L);
    }

    public int getGenesisBalancesNumber() {
        return initialGenesisAccountsBalances.size() - 1;
    }

}
