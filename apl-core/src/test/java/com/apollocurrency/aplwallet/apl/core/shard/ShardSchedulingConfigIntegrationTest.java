/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableWeld
class ShardSchedulingConfigIntegrationTest {


   PropertiesHolder  holder = new PropertiesHolder();

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(PropertyProducer.class, ShardSchedulingConfig.class)
        .addBeans(MockBean.of(holder, PropertiesHolder.class))
        .build();

    private Properties properties(Integer minDelay, Integer maxDelay, Integer maxRollback, Boolean noShardCreate) {
        Properties props = new Properties();
        putPropIfExist("apl.shard.minDelay", minDelay, props);
        putPropIfExist("apl.shard.maxDelay", maxDelay, props);
        putPropIfExist("apl.noshardcreate", noShardCreate, props);
        putPropIfExist("apl.maxRollback", maxRollback, props);
        return props;
    }

    private void putPropIfExist(String name, Object prop, Properties properties) {
        if (prop != null) {
            properties.put(name, prop.toString());
        }
    }


    @Test
    void testInitConfigFromDefaults() {
        holder.init(properties(null, null, null, null));
        ShardSchedulingConfig config = weldInitiator.container().select(ShardSchedulingConfig.class).get();

        assertTrue(config.shardDelayed());
        assertEquals(10, config.getMinDelay());
        assertEquals(60, config.getMaxDelay());
        assertEquals(720, config.getMaxRollback());
        assertTrue(config.isCreateShards());
    }

    @Test
    void testInitConfigFromCustomValues() {
        holder.init(properties(20, 30, 30, true));
        ShardSchedulingConfig config = weldInitiator.container().select(ShardSchedulingConfig.class).get();

        assertTrue(config.shardDelayed());
        assertEquals(20, config.getMinDelay());
        assertEquals(30, config.getMaxDelay());
        assertEquals(720, config.getMaxRollback());
        assertFalse(config.isCreateShards());
    }

    @Test
    void testInitConfigWithIncorrectDelay() {
        holder.init(properties(30, 30, 1000, true));
        assertThrows(IllegalArgumentException.class, () -> weldInitiator.container().select(ShardSchedulingConfig.class).get());

    }
}