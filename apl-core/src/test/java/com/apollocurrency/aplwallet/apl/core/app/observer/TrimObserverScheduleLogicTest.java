/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TrimObserverScheduleLogicTest {
    TrimService trimService = mock(TrimService.class);
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    HeightConfig config = Mockito.mock(HeightConfig.class);
    TrimConfig trimConfig = Mockito.mock(TrimConfig.class);
    Random random = new Random();
    Blockchain blockchain = mock(Blockchain.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(TrimObserver.class)
        .addBeans(MockBean.of(trimService, TrimService.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(random, Random.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(trimConfig, TrimConfig.class))
        .build();
    TrimObserver observer;

    {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(100).when(propertiesHolder).getIntProperty("apl.trimProcessingDelay", 2000);
    }

    @BeforeEach
    void setUp() {
        observer = new TrimObserver(this.trimService, this.trimConfig, this.blockchain);
    }

    @Test
    void testOnBlockPushedOne() {
        when(trimConfig.getTrimFrequency()).thenReturn(5000);
        Block block = mock(Block.class);
        when(block.getHeight()).thenReturn(5000);

        observer.onBlockPushed(block);
        observer.onBlockPushed(block);
        List<Integer> generatedTrimHeights = observer.getTrimQueue();
        assertEquals(2, generatedTrimHeights.size());
    }

    @Test
    void testOnBlockPushedTwo() {
        when(trimConfig.getTrimFrequency()).thenReturn(5000).thenReturn(6000);
        Block block = mock(Block.class);
        when(block.getHeight()).thenReturn(5000).thenReturn(6000);

        observer.onBlockPushed(block);
        observer.onBlockPushed(block);
        List<Integer> generatedTrimHeights = observer.getTrimQueue();
        assertEquals(2, generatedTrimHeights.size());
    }

}