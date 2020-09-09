/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import java.util.Comparator;
import java.util.PriorityQueue;

public class SizeBoundedPriorityQueue<T> extends PriorityQueue<T> {
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
}
