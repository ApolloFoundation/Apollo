/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirstLastIndexParser {
    private final int maxAPIrecords;

    @Inject
    public FirstLastIndexParser(@Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords) {
        this.maxAPIrecords = maxAPIrecords;
    }

    public FirstLastIndex adjustIndexes(int firstIndexParam, int lastIndexParam){
        int firstIndex = Math.min(firstIndexParam, Integer.MAX_VALUE - maxAPIrecords + 1);
        int lastIndex = Math.min(lastIndexParam, firstIndex + maxAPIrecords - 1);
        if (lastIndex < firstIndex) {
            lastIndex = firstIndex + maxAPIrecords - 1;
        }
        return new FirstLastIndex(firstIndex, lastIndex);
    }

    @Getter
    public static class FirstLastIndex{
        private final int firstIndex;
        private final int lastIndex;

        private FirstLastIndex(int firstIndex, int lastIndex) {
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
        }

        // for unit tests only !
        public FirstLastIndex(Integer firstIndex, Integer lastIndex) {
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
        }
    }
}
