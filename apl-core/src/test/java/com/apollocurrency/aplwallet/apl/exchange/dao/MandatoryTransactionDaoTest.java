/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiTransactionalSqlObjectDaoProxyInvocationHandler;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@Testcontainers
@Tag("slow")
class MandatoryTransactionDaoTest extends DbContainerBaseTest {

    static String cancelBytes = "09110b252703780070fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000e1f505000000000000000000000000000000000000000000000000000000000000000000000000898f755511cd0a3aec0128094bd87f996a90519e7f9c3b2b183f5d7def77c40ab18215a72f44aaa55ef304371180cfa5517554a87ffc65507dd8bd586226dea200000000000000001a51f385ecc580fe0180c13d459b696166";
    static String orderBytes = "09105c1f2703a00570fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000c2eb0b000000000000000000000000000000000000000000000000000000000000000000000000d323abad8bec5704995e40621026a93e29eba1b8726f4fbfb6f7fde06fd22a02135e46d5019536b0282beb549ab87e4f2a888dcdc615445c13d91253e950e18c00000000000000001a51f385ecc580fe0200000010a5d4e800000001102700000000000000db7028032a00307836303232343263363836343065373534363737623638336532306132373430663866393566376433180041504c2d4b3738572d5a374c522d54504a592d3733485a4b";
    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    DexService dexService;
    @Mock
    TimeService timeService;

    MandatoryTransactionDao dao;
    private MandatoryTransaction cancelTx;
    private MandatoryTransaction orderTx;

    @BeforeEach
    void setUp() {
        dao = JdbiTransactionalSqlObjectDaoProxyInvocationHandler.createProxy(extension.getDatabaseManager().getJdbiHandleFactory(), MandatoryTransactionDao.class);
        orderTx = new MandatoryTransaction((byte[]) null, Convert.parseHexString(orderBytes), (long) 20);
        cancelTx = new MandatoryTransaction(Convert.parseHexString("2f23970cdc290b328e922ab0de51c288066e8579237c7b0fd45add2d064f5ff6"), Convert.parseHexString(cancelBytes), (long) 10);
    }

    @Test
    void testGetById() {
        MandatoryTransaction tx = dao.get(749837771503999228L);

        assertEquals(cancelTx, tx);
    }

    @Test
    void testGetAll() {
        List<MandatoryTransaction> all = dao.getAll(0L, 3);

        assertEquals(List.of(cancelTx, orderTx), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<MandatoryTransaction> all = dao.getAll(10, 3);

        assertEquals(List.of(orderTx), all);
    }

    @Test
    void testInsert() {
        Transaction tx = mock(Transaction.class);
        doReturn(1L).when(tx).getId();
        doReturn(orderTx.getTransactionBytes()).when(tx).getCopyTxBytes();
        MandatoryTransaction newTx = new MandatoryTransaction(tx, null, orderTx.getDbEntryId() + 1);

        dao.insert(newTx);

        List<MandatoryTransaction> all = dao.getAll(0, 10);
        newTx.setTransaction(null);

        assertEquals(List.of(cancelTx, orderTx, newTx), all);
    }

    @Test
    void testDelete() {
        dao.delete(3606021951720989487L);

        List<MandatoryTransaction> all = dao.getAll(0, 3);

        assertEquals(List.of(cancelTx), all);
    }

}