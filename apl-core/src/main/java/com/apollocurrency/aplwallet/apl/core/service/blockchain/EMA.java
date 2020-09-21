/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import java.util.ArrayList;
import java.util.List;

public class EMA {
    private final List<Double> values = new ArrayList<>();
    private final List<Double> emas = new ArrayList<>();
    private final int emaPeriod;
    private final double lastValueWeight;

    public EMA(int emaPeriod) {
        this.emaPeriod = emaPeriod;
        this.lastValueWeight = 2.0 / (emaPeriod + 1);
    }

    public void add(double v) {
        values.add(v);
        calculateNext(v);

    }

    private void calculateNext(double p) {
        if (values.size() == emaPeriod) {
            emas.add(sma());
        } else if (values.size() > emaPeriod){
            Double lastEma = emas.get(emas.size() - 1);
            double newEma = lastValueWeight * p + (1 - lastValueWeight) * lastEma;
            emas.add(newEma);
        }
    }

    public double current() {
        if (emas.isEmpty()) {
            return 0;
        }
        return emas.get(emas.size() - 1);
    }

    private double sma() {
        return values.stream().limit(emaPeriod).mapToDouble(Double::doubleValue).average().orElseThrow();
    }
}
