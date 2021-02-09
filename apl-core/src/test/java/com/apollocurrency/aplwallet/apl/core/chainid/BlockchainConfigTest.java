/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.FeaturesHeightRequirement;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.inject.Produces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
public class BlockchainConfigTest {
    private static final BlockchainProperties bp0 = new BlockchainProperties(0, 0, 160, 0, 1, 2, 0, 100L);
    private static final BlockchainProperties bp1 = new BlockchainProperties(100, 0, 160, 0, 1, 2, 0, 100L);
    private static final BlockchainProperties bp2 = new BlockchainProperties(200, 0, 160, 0, 2, 4, 0, 100L);
    private static final BlockchainProperties bp3 = new BlockchainProperties(300, 0, 160, 0, 2, 4, 0, 100L);
    private static final BlockchainProperties bp4 = new BlockchainProperties(400, 0, 160, 0, 2, 3, 0, 100L);
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES = Arrays.asList(
        bp0,
        bp1,
        bp2,
        bp3,
        bp4
    );
    private final Chain chain = new Chain(UUID.randomUUID(), true, Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(),
        "test",
        "test",
        "TEST",
        "TEST",
        "Test",
        10000L, 2,
        //"data.json",
        BLOCKCHAIN_PROPERTIES, new FeaturesHeightRequirement(100, 100, 100));
  
    BlockchainConfig blockchainConfig  = new BlockchainConfig(chain, new PropertiesHolder());
    private BlockDao blockDao = mock(BlockDao.class);   
    @Inject
    BlockchainConfigUpdater blockchainConfigUpdater; 
    @Inject
    Event<Block> blockEvent;
    

    @WeldSetup
    private WeldInitiator weld =
      //WAS:  
      // WeldInitiator.from(BlockchainConfig.class, BlockchainConfigUpdater.class).addBeans(MockBean.of(blockDao, BlockDao.class)).build();
      WeldInitiator.from(BlockchainConfigUpdater.class)
              .addBeans(MockBean.of(blockDao, BlockDao.class))
              .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
              .build();

    @Test
    public void testInitBlockchainConfig() {
        blockchainConfig.updateChain(chain);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(1209600, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testInitBlockchainConfigForFeatureHeightRequirement() {
        blockchainConfig.updateChain(chain);
        assertEquals(100, blockchainConfig.getDexPendingOrdersReopeningHeight());

        chain.setFeaturesHeightRequirement(null);
        assertNull(blockchainConfig.getDexPendingOrdersReopeningHeight());

        chain.setFeaturesHeightRequirement(new FeaturesHeightRequirement());
        assertNull(blockchainConfig.getDexPendingOrdersReopeningHeight());
    }

    @Test
    void testCreateBlockchainConfig() {
        BlockchainConfig blockchainConfig = new BlockchainConfig(chain, new PropertiesHolder());
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testCreateBlockchainConfigFromEmptyChain() {
        Chain emptyChain = new Chain(UUID.randomUUID(), new ArrayList<>(), "Empty", "Empty chain", "EMP", "EM", "EMP", 10000L, 2, List.of());
        assertThrows(IllegalArgumentException.class, () -> blockchainConfig.updateChain(emptyChain));
    }

    @Test
    void testCreateBlockchainConfigFromChainWithoutZeroHeightConfig() {
        Chain chainWithoutZeroConfig = chain.copy();
        chainWithoutZeroConfig.setBlockchainProperties(Map.of(100, bp0, 200, bp1, 300, bp2));
        assertThrows(IllegalArgumentException.class, () -> blockchainConfig.updateChain(chainWithoutZeroConfig));
    }

    @Test
    void testInitBlockchainConfigWithMinPrunableLifeTime() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");

        blockchainConfig.updateChain(chain, propertiesHolder);

        assertEquals(5000, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(5000, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testInitBlockchainConfigWithMinMaxPrunableLifeTime() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(6000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");

        blockchainConfig.updateChain(chain, propertiesHolder);

        assertEquals(6000, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(5000, blockchainConfig.getMinPrunableLifetime());
        assertTrue(blockchainConfig.isEnablePruning());
    }

    @Test
    void testInitBlockchainConfigWithDisabledPruning() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(-1).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");

        blockchainConfig.updateChain(chain, propertiesHolder);

        assertEquals(Integer.MAX_VALUE, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(5000, blockchainConfig.getMinPrunableLifetime());
        assertFalse(blockchainConfig.isEnablePruning());
    }

    @Test
    void testUpdateBlockchainConfig() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");

        blockchainConfigUpdater.updateChain(chain, propertiesHolder);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(1209600, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testUpdateBlockchainConfigWithPrunableLifeTimeGreaterThanMinPrunableLifeTime() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(2000000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");

        blockchainConfigUpdater.updateChain(chain, propertiesHolder);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(2000000, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testUpdateToHeight() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        blockchainConfigUpdater.updateToHeight(99);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(Optional.empty(), blockchainConfig.getPreviousConfig());

        blockchainConfigUpdater.updateToHeight(100);
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(201);
        assertEquals(new HeightConfig(bp2, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(199);
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(200);
        assertEquals(new HeightConfig(bp2, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(0);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(Optional.empty(), blockchainConfig.getPreviousConfig());

        blockchainConfigUpdater.updateToHeight(305);
        assertEquals(new HeightConfig(bp3, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp2, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(401);
        assertEquals(new HeightConfig(bp4, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp3, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.updateToHeight(221);
        assertEquals(new HeightConfig(bp2, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

    }

    @Test
    void testRollback() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        blockchainConfigUpdater.updateToHeight(102);
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.rollback(100);
        assertEquals(new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getPreviousConfig().get());

        blockchainConfigUpdater.rollback(99);
        assertEquals(new HeightConfig(bp0, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply()), blockchainConfig.getCurrentConfig());
        assertEquals(Optional.empty(), blockchainConfig.getPreviousConfig());
    }

    @Test
    public void testChangeListenerOnBlockAccepted() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        Block block = mock(Block.class);
        Mockito.doReturn(100).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
        HeightConfig result = blockchainConfig.getCurrentConfig();
        HeightConfig expected = new HeightConfig(bp1, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply());
        assertEquals(expected, result);
    }

    @Test
    public void testChangeListenerOnPopped() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        Block block = mock(Block.class);
        Mockito.doReturn(201).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.BLOCK_POPPED)).fire(block);
        HeightConfig result = blockchainConfig.getCurrentConfig();
        HeightConfig expected = new HeightConfig(bp2, blockchainConfig.getOneAPL(), blockchainConfig.getInitialSupply());
        assertEquals(expected, result);
    }

    private AnnotationLiteral literal(BlockEventType blockEvent) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEvent;
            }
        };
    }
}
