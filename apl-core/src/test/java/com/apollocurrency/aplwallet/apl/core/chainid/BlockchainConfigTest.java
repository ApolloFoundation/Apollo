/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

@EnableWeld
public class BlockchainConfigTest {
    private BlockDao blockDao = Mockito.mock(BlockDao.class);
    @WeldSetup
    private WeldInitiator weld =
            WeldInitiator.from(BlockchainConfig.class, BlockchainConfigUpdater.class).addBeans(MockBean.of(blockDao, BlockDao.class)).build();
    private static final BlockchainProperties bp1 = new BlockchainProperties(0, 0, 1, 0, 0, 100L);
    private static final BlockchainProperties bp2 = new BlockchainProperties(100, 0, 1, 0, 0, 100L);
    private static final BlockchainProperties bp3 = new BlockchainProperties(200, 0, 2, 0, 0, 100L);
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
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testUpdateBlockchainConfig() {
        blockchainConfigUpdater.updateChain(chain);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testUpdateToHeight() {
        blockchainConfigUpdater.updateChain(chain);
        blockchainConfigUpdater.updateToHeight(99, true);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(100, true);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(100, false);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(201, true);
        Assertions.assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(0, false);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.updateToHeight(0, true);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    void testRollback() {
        blockchainConfigUpdater.updateChain(chain);
        blockchainConfigUpdater.updateToHeight(102, true);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(100);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());
        blockchainConfigUpdater.rollback(99);
        Assertions.assertEquals(new HeightConfig(bp1), blockchainConfig.getCurrentConfig());
    }

    @Test
    public void testChangeListenerOnBlockAccepted() {
        blockchainConfigUpdater.updateChain(chain);
        Block block = Mockito.mock(Block.class);
        Mockito.doReturn(100).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
        Assertions.assertEquals(new HeightConfig(bp2), blockchainConfig.getCurrentConfig());

    }
    @Test
    public void testChangeListenerOnPopped() {
        blockchainConfigUpdater.updateChain(chain);
        Block block = Mockito.mock(Block.class);
        Mockito.doReturn(200).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.BLOCK_POPPED)).fire(block);
        Assertions.assertEquals(new HeightConfig(bp3), blockchainConfig.getCurrentConfig());

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
