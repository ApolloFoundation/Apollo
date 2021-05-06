/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;


import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ToString
public class IdQueue<T> implements Queue<T> {
    private final Queue<T> queue;
    private final Set<Long> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Function<T, Long> hasher;
    private final int maxSize;

    public IdQueue(Queue<T> queue, Function<T, Long> hasher, int maxSize) {
        this.queue = queue;
        this.hasher = hasher;
        this.maxSize = maxSize;
    }
    public IdQueue(Queue<T> queue) {
        this(queue, t -> (long) t.hashCode(), 2048);
    }

    @Override
    public synchronized boolean add(T t) {
        if (ids.size() == maxSize) {
            return false;
        }
        Long hash = hasher.apply(t);
        boolean add = ids.add(hash);
        if (!add) {
            return false;
        }
        try {
            boolean added = queue.add(t);
            if (!added) {
                ids.remove(hash);
            }
            return added;
        } catch (RuntimeException e) {
            ids.remove(hash);
            throw e;
        }
    }

    public synchronized ReturnCode addWithStatus(T t) {
        if (ids.size() == maxSize) {
            return ReturnCode.FULL;
        }
        Long hash = hasher.apply(t);
        boolean add = ids.add(hash);
        if (!add) {
            return ReturnCode.ALREADY_EXIST;
        }
        try {
            boolean added = queue.add(t);
            if (!added) {
                ids.remove(hash);
            }
            return added ? ReturnCode.ADDED : ReturnCode.NOT_ADDED;
        } catch (RuntimeException e) {
            ids.remove(hash);
            throw e;
        }
    }

    @Override
    public synchronized boolean offer(T t) {
        if (ids.size() == maxSize) {
            return false;
        }
        Long hash = hasher.apply(t);
        boolean add = ids.add(hash);
        if (!add) {
            return false;
        }
        try {
            boolean added = queue.offer(t);
            if (!added) {
                ids.remove(hash);
            }
            return added;
        } catch (RuntimeException e) {
            ids.remove(hash);
            throw e;
        }
    }

    @Override
    public synchronized T remove() {
        T remove = queue.remove();
        if (remove != null) {
            Long apply = hasher.apply(remove);
            ids.remove(apply);
        }
        return remove;
    }

    @Override
    public synchronized T poll() {
        T remove = queue.poll();
        if (remove != null) {
            Long apply = hasher.apply(remove);
            ids.remove(apply);
        }
        return remove;
    }

    @Override
    public synchronized boolean contains(Object o) {
        Objects.requireNonNull(o);
        Long hash = hasher.apply((T) o);
        return ids.contains(hash);
    }

    public synchronized boolean contains(Long hash) {
        return ids.contains(hash);
    }

    @Override
    public synchronized T element() {
        return queue.element();
    }

    @Override
    public synchronized T peek() {
        return queue.peek();
    }

    @Override
    public synchronized int size() {
        return ids.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public synchronized Iterator<T> iterator() {
        Iterator<T> iterator = queue.iterator();
        return new Iterator<T>() {
            T current;
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                T next = iterator.next();
                current = next;
                return next;
            }

            @Override
            public void remove() {
                iterator.remove();
                ids.remove(hasher.apply(current));
            }
        };
    }

    @Override
    public synchronized Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public synchronized  <T1> T1[] toArray(T1[] a) {
        return queue.toArray(a);
    }

    @Override
    public synchronized <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return queue.toArray(generator);
    }

    @Override
    public synchronized boolean remove(Object o) {
        Objects.requireNonNull(o);
        boolean contains = contains(o);
        if (contains) {
            ids.remove(hasher.apply((T) o));
            return queue.remove(o);
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        boolean modified = false;
        for (T t : c) {
            if (add(t)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public synchronized boolean removeIf(Predicate<? super T> filter) {
        return queue.removeIf(filter);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("RetailALL is not supported for IdQueue");
    }

    @Override
    public synchronized void clear() {
        queue.clear();
        ids.clear();
    }

    @Override
    public synchronized boolean equals(Object o) {
        return queue.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return queue.hashCode();
    }

    @Override
    public synchronized Spliterator<T> spliterator() {
        return queue.spliterator();
    }

    @Override
    public synchronized Stream<T> stream() {
        return queue.stream();
    }

    @Override
    public synchronized Stream<T> parallelStream() {
        return queue.parallelStream();
    }

    @Override
    public synchronized void forEach(Consumer<? super T> action) {
        queue.forEach(action);
    }
    public enum ReturnCode {
        ALREADY_EXIST, FULL, NOT_ADDED, ADDED;

        public boolean isOk() {
            return this == ADDED;
        }
    }
}
