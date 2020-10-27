/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class ReferencedTransactionMigratorTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension dbExtension = new DbExtension(mariaDBContainer);
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(
        ChainsConfigHolder.class, BlockchainConfig.class, FullTextConfigImpl.class,
        TransactionRowMapper.class,
        TransactionBuilder.class,
        DerivedDbTablesRegistryImpl.class,
        ReferencedTransactionDaoImpl.class)
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(Mockito.mock(Blockchain.class), BlockchainImpl.class))
        .addBeans(MockBean.of(Mockito.mock(TimeService.class), TimeService.class, TimeServiceImpl.class))
        .addBeans(MockBean.of(Mockito.mock(PropertiesHolder.class), PropertiesHolder.class))
        .addBeans(MockBean.of(Mockito.mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();

    @Inject
    ReferencedTransactionDaoImpl referencedTransactionDao;

    private ReferencedTransactionMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new ReferencedTransactionMigrator(dbExtension.getDatabaseManager());
    }

    @Test
    void testMigrate() throws SQLException {

        try (Connection connection = dbExtension.getDatabaseManager().getDataSource().getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("update referenced_transaction set height = -1 where db_id >= 40");
        }
        List<ReferencedTransaction> notMigrated = CollectionUtil.toList(referencedTransactionDao.getManyBy(new DbClause.LongClause("db_id", DbClause.Op.GTE, 40), 0, Integer.MAX_VALUE));
        notMigrated.forEach(rtx -> assertEquals(-1, rtx.getHeight()));

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
