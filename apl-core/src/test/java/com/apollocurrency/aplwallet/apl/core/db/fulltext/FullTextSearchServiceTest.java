package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

@Disabled
@EnableWeld
class FullTextSearchServiceTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockDaoTestDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class,
            PropertiesConfigLoader.class, GlobalSyncImpl.class, PropertyProducer.class,
            PropertiesHolder.class, BlockchainImpl.class, DbConfig.class, DaoConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class, TransactionIndexDao.class,
            JdbiHandleFactory.class,
            DerivedDbTablesRegistryImpl.class, FullTextConfigProducer.class, FullTextConfigImpl.class,
            LuceneFullTextSearchEngine.class, FullTextSearchServiceImpl.class,
            BlockchainConfigUpdater.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;

    private static PropertiesHolder propertiesHolder;
    @Inject
    private PropertyProducer propertyProducer;
    @Inject
    private FullTextConfigProducer textConfigProducer;
    @Inject
    private FullTextSearchService searchService;
    @Inject
    private LuceneFullTextSearchEngine fullTextSearchEngine;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @AfterEach
    void tearDown() {
        jdbiHandleFactory.close();
//        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @BeforeEach
    void setUp() {
        propertyProducer = new PropertyProducer(propertiesHolder);

//        textConfigProducer = new FullTextConfigProducer();
        searchService = new FullTextSearchServiceImpl(fullTextSearchEngine,
                textConfigProducer.produceFullTextTables(), textConfigProducer.produceTablesSchema());
    }

    @Test
    void init() {
        searchService.init();
    }
}