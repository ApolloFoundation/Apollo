/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableWeld
@Disabled
class MandatoryTransactionDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    static String cancelBytes = "09110b252703780070fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000e1f505000000000000000000000000000000000000000000000000000000000000000000000000898f755511cd0a3aec0128094bd87f996a90519e7f9c3b2b183f5d7def77c40ab18215a72f44aaa55ef304371180cfa5517554a87ffc65507dd8bd586226dea200000000000000001a51f385ecc580fe0180c13d459b696166";
    static String orderBytes = "09105c1f2703a00570fa32fa006ba1ff67b9809f9b8dd74e0ee5de84ff4834408c106980a8b05f034add89a5076a2218000000000000000000c2eb0b000000000000000000000000000000000000000000000000000000000000000000000000d323abad8bec5704995e40621026a93e29eba1b8726f4fbfb6f7fde06fd22a02135e46d5019536b0282beb549ab87e4f2a888dcdc615445c13d91253e950e18c00000000000000001a51f385ecc580fe0200000010a5d4e800000001102700000000000000db7028032a00307836303232343263363836343065373534363737623638336532306132373430663866393566376433180041504c2d4b3738572d5a374c522d54504a592d3733485a4b";

    private MandatoryTransaction cancelTx;
    private MandatoryTransaction orderTx;

    @WeldSetup
    WeldInitiator weld =  WeldUtils.from(List.of(MandatoryTransactionDao.class, DaoConfig.class),
            List.of(BlockchainConfig.class, Blockchain.class, DexService.class, PropertiesHolder.class, TimeService.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .build();

    @Inject
    MandatoryTransactionDao dao;

    @BeforeEach
    void setUp() {
        try {
            orderTx = new MandatoryTransaction(Transaction.newTransactionBuilder(Convert.parseHexString(orderBytes)).build(), null, (long) 20);
            cancelTx = new MandatoryTransaction(Transaction.newTransactionBuilder(Convert.parseHexString(cancelBytes)).build(), orderTx.getFullHash(), (long) 10);
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetById() {
        MandatoryTransaction tx = dao.get(cancelTx.getId());

        assertEquals(cancelTx, tx);
    }

    @Test
    void testGetAll() {
        List<MandatoryTransaction> all = dao.getAll(0L, 3);

        assertEquals(List.of(cancelTx, orderTx), all);
    }

    @Test
    void testGetAllWithPagination() {
        List<MandatoryTransaction> all = dao.getAll(cancelTx.getDbEntryId(), 3);

        assertEquals(List.of(orderTx), all);
    }

    @Test
    void testInsert() {
        MandatoryTransaction newTx = new MandatoryTransaction(orderTx.getTransaction(), null, orderTx.getDbEntryId() + 1);

        dao.insert(newTx);

        List<MandatoryTransaction> all = dao.getAll(0, 10);
        assertEquals(List.of(cancelTx, orderTx, newTx), all);
    }

    @Test
    void testDelete() {
        dao.delete(orderTx.getId());

        List<MandatoryTransaction> all = dao.getAll(0, 3);

        assertEquals(List.of(cancelTx), all);
    }

}