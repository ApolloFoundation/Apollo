/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.TestData;
import dto.Account;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.WalletRunner;

import java.io.IOException;
import java.util.List;

public class GetGenesisBalancesTest {
    private static WalletRunner runner = new WalletRunner();
    private NodeClient client = new NodeClient();
    @BeforeClass
    public static void init() throws IOException {
        runner.run();
        runner.disableReloading();
    }

    @AfterClass
    public static void tearDown() {
        runner.shutdown();
    }

    @Test
    public void testGetGenesisBalancesWithPagination() throws IOException {
        List<Account> genesisBalances = client.getGenesisBalances(TestData.TEST_LOCALHOST, 10, 20);
        Assert.assertNotNull(genesisBalances);
        Assert.assertEquals(11, genesisBalances.size());
        checkOrder(genesisBalances);
    }

    @Test
    public void testGetGenesisBalancesWithWhenLastIndexLessThanFirstIndex() throws IOException {
        String errorJson = client.getGenesisBalancesJSON(TestData.TEST_LOCALHOST, 10, 9);
        JsonFluentAssert.assertThatJson(errorJson)
                .isPresent()
                .node("error")
                .isPresent()
                .isString();
        Assert.assertTrue(errorJson.contains("IllegalArgumentException"));
    }

    @Test
    public void testGetGenesisBalancesWithWhenLastIndexAndFirstIndexGreaterThanNumberOfGenesisAccounts() throws IOException {
        String errorJson = client.getGenesisBalancesJSON(TestData.TEST_LOCALHOST, Integer.MAX_VALUE, Integer.MAX_VALUE);
        JsonFluentAssert.assertThatJson(errorJson)
                .isPresent()
                .node("error")
                .isPresent()
                .isString();
        Assert.assertTrue(errorJson.contains("IllegalArgumentException"));
    }

    @Test
    public void testGetGenesisBalancesWithoutPagination() throws IOException {
        List<Account> genesisBalances = client.getGenesisBalances(TestData.TEST_LOCALHOST);
        Assert.assertNotNull(genesisBalances);
        Assert.assertEquals(100, genesisBalances.size());
        checkOrder(genesisBalances);
    }

    private void checkOrder(List<Account> genesisBalances) {
        long prevBalance = Long.MAX_VALUE;
        for (Account genesisBalance : genesisBalances) {
            if (prevBalance < genesisBalance.getBalanceATM()) {
                Assert.fail("Genesis balances were not ordered");
            } else {
                prevBalance = genesisBalance.getBalanceATM();
            }
        }
    }
}
