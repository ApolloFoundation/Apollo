/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer.events;

/**
 * Enum class for different node states while it does search for shard present information
 */
public enum ShardPresentEventType {
    NO_SHARD, // it's definitely NO shards present in network
    PRESENT // network contains some nodes with shard, so shard is present actually
}
