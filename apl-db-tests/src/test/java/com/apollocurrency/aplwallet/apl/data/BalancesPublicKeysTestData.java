/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BalancesPublicKeysTestData {

    public Set<String> publicKeySet;
    public Map<Long, Long> balances;

    public BalancesPublicKeysTestData() {
        // that set corresponds 'publicKeys' in  /resources/conf/data/genesisAccounts-testnet.json
        this.publicKeySet = Set.of(
            "5e8d43ff197f8b554a59007f9e6f73e10bff4dda9906f8389d015f31d0abc433",
            "25ae5107a806e561488394ed5b59916d61c2f0110182e67a1aae19cd6bd86d0e",
            "7ad7781e9fd7efd304e516374e816e09356db8439d8d577da02a5b3ec55d6274",
            "58f5675bde9dc62ec712988e84f336c1859ce672a065706c2d6b53f24809073a",
            "42edd0892c113954cc9efd503e424e79d60d2ec94356f9c65bf7ea651f3a2e6a",
            "1ab245bba7d381256999a016f40fe44ec7e3fe15ef598a3f4b089b4ebc40e973",
            "840544113081fd01265f6881dc4bf0627fd06e3a9d9abf1f2fc65042ac03145d",
            "638ea31bc014e00b858d3eac8cb5b1bed168ea8290b4cbae6e6de0498abad557",
            "2308a0680a8390abd47f3afe616f604047dcdd5a05e4eb1877dd9332cd56a057",
            "af787ee65f2ce7355d10b3d69390bb48bbd47b725f2eb0c786f9d9e623a1ac51");

        // that map corresponds 'balances' in /resources/conf/data/genesisAccounts-testnet.json
        this.balances = new HashMap<>(10);
        this.balances.put(1L, 15984639707L);
        this.balances.put(10L, 3303638046L);
        this.balances.put(10000L, 300338008L);
        this.balances.put(100000L, 30034875133418L);
        this.balances.put(1000000002807863236L, 900992194L);
        this.balances.put(Long.parseUnsignedLong("10000061009900011000"), 13410532026968L);
        this.balances.put(Long.parseUnsignedLong("10000359721367068868"), 11671903383L);
        this.balances.put(Long.parseUnsignedLong("10000810790688464705"), 134247836976L);
        this.balances.put(Long.parseUnsignedLong("10001032570457574668"), 65374652437L);
        this.balances.put(Long.parseUnsignedLong("10001413517050099775"), 1201322925L);
    }

}
