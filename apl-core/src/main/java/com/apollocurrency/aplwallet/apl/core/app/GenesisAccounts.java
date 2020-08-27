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
    private static List<Map.Entry<String, Long>> initialGenesisAccountsBalances;
    private static GenesisImporter genesisImporter;

    public static void init() throws GenesisImportException {
        if (genesisImporter == null) {
            genesisImporter = CDI.current().select(GenesisImporter.class).get();
        }
        initialGenesisAccountsBalances = genesisImporter.loadGenesisAccounts();
    }

    public static List<Map.Entry<String, Long>> getGenesisBalances(int firstIndex, int lastIndex) {
        firstIndex = Math.max(firstIndex, 0);
        lastIndex = Math.max(lastIndex, 0);
        if (lastIndex < firstIndex) {
            throw new IllegalArgumentException("firstIndex should be less or equal lastIndex ");
        }
        if (firstIndex >= initialGenesisAccountsBalances.size() || lastIndex > initialGenesisAccountsBalances.size()) {
            throw new IllegalArgumentException("firstIndex and lastIndex should be less than " + initialGenesisAccountsBalances.size());
        }
        if (lastIndex - firstIndex > 99) {
            lastIndex = firstIndex + 99;
        }
        return initialGenesisAccountsBalances.subList(firstIndex, lastIndex + 1);
    }

    public static int getGenesisBalancesNumber() {
        return initialGenesisAccountsBalances.size();
    }

}
