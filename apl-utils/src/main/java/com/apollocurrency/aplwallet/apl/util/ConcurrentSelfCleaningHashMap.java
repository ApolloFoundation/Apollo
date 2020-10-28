/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ConcurrentSelfCleaningHashMap<K,V> extends ConcurrentHashMap<K, ConcurrentSelfCleaningHashMap.ValueEntry> {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("SelfClearingHashmap"));
    private final int accessRemoveDelay;
    private final int modificationRemoveDelay;
    private final Predicate<V> valueRemovePredicate;

    public ConcurrentSelfCleaningHashMap(int afterAccessRemoveDelay, int afterModificationRemoveDelay, Predicate<V> valueRemovePredicate) {
        executor.scheduleWithFixedDelay(this::cleanup, 5000, 5000, TimeUnit.MILLISECONDS);
        this.accessRemoveDelay = afterAccessRemoveDelay;
        this.modificationRemoveDelay = afterModificationRemoveDelay;
        this.valueRemovePredicate = valueRemovePredicate;
    }


    private void cleanup() {
        Set<K> keysToRemove = new HashSet<>();
        super.entrySet().forEach(e-> {
            long currentTime = System.currentTimeMillis();
            ValueEntry valueEntry = e.getValue();
            synchronized (valueEntry) {
                V value = valueEntry.getValue();
                if (accessRemoveDelay > 0 && currentTime - valueEntry.getAccessTime() > accessRemoveDelay) {
                    keysToRemove.add(e.getKey());
                } else if (modificationRemoveDelay > 0 && currentTime - valueEntry.getModificationTime() > modificationRemoveDelay) {
                    keysToRemove.add(e.getKey());
                } else if (valueRemovePredicate != null && valueRemovePredicate.test(value)) {
                    keysToRemove.add(e.getKey());
                }
            }
        });
        keysToRemove.forEach(this::remove);
    }


    @Data
    class ValueEntry {
        private volatile long modificationTime = System.currentTimeMillis();
        private volatile long accessTime = System.currentTimeMillis();
        private volatile V value;

        public ValueEntry(V value) {
            this.value = value;
        }
    }

    public V getValue(K key) {
        ValueEntry valueEntry = get(key);
        if (valueEntry != null) {
            synchronized (valueEntry) {
                valueEntry.accessTime = System.currentTimeMillis();
                return valueEntry.value;
            }
        } else {
            return null;
        }
    }

    public V putValue(K key, V value) {
        ValueEntry valueEntry = get(key);
        if (valueEntry == null) {
            put(key, new ValueEntry(value));
            return null;
        } else {
            synchronized (valueEntry) {
                V prev = valueEntry.value;
                valueEntry.value = value;
                valueEntry.modificationTime = System.currentTimeMillis();
                return prev;
            }
        }
    }
}
