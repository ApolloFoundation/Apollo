/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.apollocurrency.aplwallet.apl.util.LockUtils;
import com.google.common.base.Preconditions;
import com.google.common.cache.AbstractCache;
import com.google.common.cache.CacheStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@ToString
public class SynchronizedCache<T, V> extends AbstractCache<T, V> {
    private final Map<T, TimedEntity> map = new HashMap<>();
    private final Queue<TimedEntity> lruQueue = new PriorityQueue<>(Comparator.comparing(TimedEntity::getTime));
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final SimpleStatsCounter statsCounter = new SimpleStatsCounter();
    private final int maxSize;


    public SynchronizedCache(int maxSize) {
        Preconditions.checkArgument(maxSize > 0, "Required maxSize to be greater than zero, got " + maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public long size() {
        return map.size();
    }

    @Override
    public @Nullable V getIfPresent(@NonNull Object key) {
        return LockUtils.getInLock(lock.readLock(), ()->{
            TimedEntity v = map.get(key);
            if (v != null) {
                v.setTime(System.nanoTime());
                statsCounter.recordHits(1);
                return v.getEntity();
            } else {
                statsCounter.recordMisses(1);
                return null;
            }
        });
    }

    public long maxSize() {
        return maxSize;
    }

    @Override
    public CacheStats stats() {
        return statsCounter.snapshot();
    }

    @Override
    public void put(@NonNull T key, @NonNull V value) {
        LockUtils.doInLock(lock.writeLock(), () -> {
            if (maxSize == map.size() && !map.containsKey(key)) {
                TimedEntity timedEntityToRemove = lruQueue.remove();
                TimedEntity removed = map.remove(timedEntityToRemove.key);
                Preconditions.checkState(removed != null, "Inconsistent cache state, least recent " +
                    "element %s, should exist in the cache map, ", timedEntityToRemove);
                statsCounter.recordEviction();
            }
            TimedEntity valueToAdd = new TimedEntity(key, value, System.nanoTime());
            TimedEntity previous = map.put(key, valueToAdd);
            if (previous != null) {
                lruQueue.remove(previous);
            }
            lruQueue.add(valueToAdd);
        });
    }

    @Override
    public ConcurrentMap<T, V> asMap() {
        return LockUtils.getInLock(lock.readLock(), () -> new ConcurrentHashMap<>(
            map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> e.getValue().getEntity()))));
    }

    @Override
    public void invalidate(@NonNull Object key) {
        LockUtils.doInLock(lock.writeLock(), ()-> {
            TimedEntity entity = map.remove(key);
            lruQueue.remove(entity);
        });
    }


    @Override
    public void invalidateAll() {
        LockUtils.doInLock(lock.writeLock(), ()-> {
            map.clear();
            lruQueue.clear();
        });
    }

    @AllArgsConstructor
    @Getter
    @ToString
    private class TimedEntity {
        private final T key;
        private final V entity;
        @Setter
        private volatile long time;
    }
}
