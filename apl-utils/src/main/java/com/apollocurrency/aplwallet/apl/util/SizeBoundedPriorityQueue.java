/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class SizeBoundedPriorityQueue<T> extends PriorityBlockingQueue<T> {
    private final int maxSize;
    public SizeBoundedPriorityQueue(int maxSize, Comparator<? super T> comparator) {
        super(maxSize, comparator);
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T t) {
        if (!super.add(t)) {
            return false;
        }
        if (this.size() > maxSize) {
            remove();
        }
        return true;
    }

    @Override
    public boolean offer(T t) {
        if (!super.offer(t)) {
            return false;
        }
        if (this.size() > maxSize) {
            remove();
        }
        return true;
    }
}
