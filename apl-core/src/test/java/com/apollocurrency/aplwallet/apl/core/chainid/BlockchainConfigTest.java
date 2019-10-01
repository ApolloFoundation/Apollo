/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
public class BlockchainConfigTest {
    private BlockDao blockDao = mock(BlockDao.class);
    @WeldSetup
    private WeldInitiator weld =
            WeldInitiator.from(BlockchainConfig.class, BlockchainConfigUpdater.class).addBeans(MockBean.of(blockDao, BlockDao.class)).build();
    private static final BlockchainProperties bp1 = new BlockchainProperties(0, 0, 1, 0, 0, 100L, new FeaturesHeightRequirement(0));
    private static final BlockchainProperties bp2 = new BlockchainProperties(100, 0, 1, 0, 0, 100L, new FeaturesHeightRequirement(0));
    private static final BlockchainProperties bp3 = new BlockchainProperties(200, 0, 2, 0, 0, 100L, new FeaturesHeightRequirement(0));
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES = Arrays.asList(
            bp1,
            bp2,
            bp3
    );



    private static final Chain chain = new Chain(UUID.randomUUID(), true, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(),
            "test",
            "test",
            "TEST",
            "TEST", "Test", "data.json", BLOCKCHAIN_PROPERTIES);

    @Inject
    BlockchainConfig blockchainConfig;
    @Inject
    BlockchainConfigUpdater blockchainConfigUpdater;
    @Inject
    Event<Block> blockEvent;

    @Test
    public void testInitBlockchainConfig() {
        blockchainConfig.updateChain(chain);
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        assertEquals(1209600, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testCreateBlockchainConfig() {
        BlockchainConfig blockchainConfig = new BlockchainConfig(chain, new PropertiesHolder());
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testCreateBlockchainConfigFromEmptyChain() {
        Chain emptyChain = new Chain(UUID.randomUUID(), new ArrayList<>(), "Empty", "Empty chain", "EMP", "EM", "EMP", "", List.of());
        assertThrows(IllegalArgumentException.class, () -> blockchainConfig.updateChain(emptyChain));
    }
    @Test
    void testCreateBlockchainConfigFromChainWithoutZeroHeightConfig() {
        Chain chainWithoutZeroConfig = chain.copy();
        chainWithoutZeroConfig.setBlockchainProperties(Map.of(100, bp1, 200, bp2, 300 , bp3));
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
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        assertEquals(1209600, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testUpdateBlockchainConfigWithPrunableLifeTimeGreaterThanMinPrunableLifeTime() {
        PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
        doReturn(2000000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");

        blockchainConfigUpdater.updateChain(chain, propertiesHolder);
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        assertEquals(2000000, blockchainConfig.getMaxPrunableLifetime());
        assertEquals(1209600, blockchainConfig.getMinPrunableLifetime());
    }

    @Test
    void testUpdateToHeight() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        blockchainConfigUpdater.updateToHeight(99);
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(100);
        assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(201);
        assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(199);
        assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(200);
        assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(0);
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testRollback() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        blockchainConfigUpdater.updateToHeight(102);
        assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(100);
        assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(99);
        assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    public void testChangeListenerOnBlockAccepted() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        Block block = mock(Block.class);
        Mockito.doReturn(100).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
        assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());

    }
    @Test
    public void testChangeListenerOnPopped() {
        blockchainConfigUpdater.updateChain(chain, new PropertiesHolder());
        Block block = mock(Block.class);
        Mockito.doReturn(201).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.BLOCK_POPPED)).fire(block);
        assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());

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
