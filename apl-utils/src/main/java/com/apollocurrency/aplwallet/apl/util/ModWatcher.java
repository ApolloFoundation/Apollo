/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

/**
 * This class replaces % operation when counter may change for more then 1
 *
 * @author alukin@gmail.com
 */
public class ModWatcher {
    int modulos_m;
    long prevH = 0;

    ModWatcher(int modulos) {
        this.modulos_m = modulos;
    }

    public int watch(int minutes) {
        int late = -1;
        int m = minutes % modulos_m;
        long h = (minutes / modulos_m);
        if (h > prevH) {
            prevH = h;
            late = m;
        }
        return late;
    }

}
