package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

@Slf4j
@EnableWeld
@Execution(ExecutionMode.SAME_THREAD) //for better performance we will not recreate 3 datasources for each test method
class GenesisImporterTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("genesisImport").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainConfigUpdater blockchainConfigUpdater = mock(BlockchainConfigUpdater.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private ConfigDirProvider configDirProvider = mock(ConfigDirProvider.class);
    private AplAppStatus aplAppStatus = mock(AplAppStatus.class);

    @WeldSetup
    public WeldInitiator weld  = WeldInitiator.from()
            .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(mock(EpochTime.class), EpochTime.class))
            .addBeans(MockBean.of(configDirProvider, ConfigDirProvider.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchainConfigUpdater, BlockchainConfigUpdater.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(aplAppStatus, AplAppStatus.class))
            .build();

    //    @Inject
    private GenesisImporter genesisImporter;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    void setUp() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        doReturn(resourceFileLoader.getResourcePath().toString()).when(chain).getGenesisLocation();
        genesisImporter = new GenesisImporter(blockchainConfig, configDirProvider,
                blockchainConfigUpdater, extension.getDatabaseManager(), aplAppStatus);
    }

    @Test
    void newGenesisBlock() {
        Block block = genesisImporter.newGenesisBlock();
        assertNotNull(block);
    }

    @Test
    void applyTrue() {
        genesisImporter.apply(true);
    }

    @Test
    void applyFalse() {
        genesisImporter.apply(false);
    }

    @Test
    void loadGenesisAccounts() {
        List<Map.Entry<String, Long>> result = genesisImporter.loadGenesisAccounts();
        assertNotNull(result);
    }
}