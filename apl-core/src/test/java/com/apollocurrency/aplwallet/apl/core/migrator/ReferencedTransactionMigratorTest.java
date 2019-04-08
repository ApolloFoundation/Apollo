/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@EnableWeld
public class ReferencedTransactionMigratorTest {
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ReferencedTransactionDaoImpl.class, BlockchainConfig.class, FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class)
            .addBeans(MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(Mockito.mock(Blockchain.class), BlockchainImpl.class))
            .addBeans(MockBean.of(Mockito.mock(EpochTime.class), EpochTime.class))
            .build();
    @Inject
    ReferencedTransactionDaoImpl referencedTransactionDao;

    private ReferencedTransactionMigrator migrator;
    private TransactionTestData td;

    @BeforeEach
    void setUp() {
        migrator = new ReferencedTransactionMigrator(dbExtension.getDatabaseManger());
        td = new TransactionTestData();
    }

    @Test
    public void testMigrate() throws SQLException {

        try (Connection connection = dbExtension.getDatabaseManger().getDataSource().getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("update referenced_transaction set height = -1 where db_id >= 40");
        }
        List<ReferencedTransaction> notMigrated = CollectionUtil.toList(referencedTransactionDao.getManyBy(new DbClause.LongClause("db_id", DbClause.Op.GTE, 40), 0, Integer.MAX_VALUE));
        notMigrated.forEach(rtx-> assertEquals(-1, rtx.getHeight().intValue()));

        migrator.migrate();

        List<ReferencedTransaction> referencedTransactions = CollectionUtil.toList(referencedTransactionDao.getAll(0, Integer.MAX_VALUE));
        List<ReferencedTransaction> expected = td.REFERENCED_TRANSACTIONS.stream().sorted(Comparator.comparing(ReferencedTransaction::getHeight).thenComparing(ReferencedTransaction::getDbId).reversed()).collect(Collectors.toList());
        assertEquals(referencedTransactions, expected);
    }
    @Test
    void testMigrateNothing() {
        migrator.migrate();

        List<ReferencedTransaction> referencedTransactions = CollectionUtil.toList(referencedTransactionDao.getAll(0, Integer.MAX_VALUE));
        List<ReferencedTransaction> expected = td.REFERENCED_TRANSACTIONS.stream().sorted(Comparator.comparing(ReferencedTransaction::getHeight).thenComparing(ReferencedTransaction::getDbId).reversed()).collect(Collectors.toList());
        assertEquals(referencedTransactions, expected);
    }
}
