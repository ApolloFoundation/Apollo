/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.vm;

import com.apollocurrency.smc.contract.fuel.FreeOperationPrice;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;
import com.apollocurrency.smc.contract.fuel.MemCostFuelCalculator;
import com.apollocurrency.smc.contract.fuel.OperationPrice;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SMCOperationPriceProvider implements PriceProvider {
    private static final PriceProvider INSTANCE = new SMCOperationPriceProvider();
    private static final Map<Integer, OperationPrice> prices = Map.of(
        0, new FreeOperationPrice(),
        1, new BaseOperationPriceAt1()
    );

    public static PriceProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public OperationPrice getPrice(int targetHeight) {
        var price = prices.get(targetHeight);
        if (price != null) {
            return price;
        }
        Optional<Integer> maxHeight = prices
            .keySet()
            .stream()
            .filter(height -> targetHeight >= height)
            .max(Comparator.naturalOrder());

        return maxHeight
            .map(prices::get)
            .orElseThrow(() -> new IllegalStateException("Can't determine the smart-contract execution price at height=" + targetHeight));
    }

    private static class BaseOperationPriceAt1 implements OperationPrice {

        @Override
        public FuelCalculator contractPublishing() {
            return FuelCost.F_PUBLISH;
        }

        @Override
        public FuelCalculator contractCreation() {
            return FuelCost.F_CREATE_CONTRACT;
        }

        @Override
        public FuelCalculator methodCalling(BigInteger value) {
            //the charge for contract method calling
            FuelCalculator fuelCalculator = new MemCostFuelCalculator(FuelCost.F_CALL);
            if (value.signum() > 0) {
                fuelCalculator.add(FuelCost.F_SEND_MONEY);
            }
            return fuelCalculator;
        }

        @Override
        public FuelCalculator sendMessage() {
            return FuelCost.F_SEND_MESSAGE;
        }

        @Override
        public FuelCalculator mathFunctionUsing() {
            return FuelCost.F_HASH_SUM;
        }

        @Override
        public FuelCalculator writeState() {
            return FuelCost.F_STATE_WRITE;
        }

        @Override
        public FuelCalculator createMapping() {
            return FuelCost.F_MAPPING_CREATE;
        }

        @Override
        public FuelCalculator readMapping() {
            return FuelCost.F_MAPPING_READ;
        }

        @Override
        public FuelCalculator writeMapping() {
            return FuelCost.F_MAPPING_WRITE;
        }

        @Override
        public FuelCalculator deleteMapping() {
            return FuelCost.F_MAPPING_DELETE;
        }

        @Override
        public FuelCalculator emitEvent() {
            return FuelCost.F_EVENT_EMIT;
        }

        @Override
        public FuelCalculator onStatementEnter() {
            return FuelCost.F_STATEMENT_ENTER;
        }
    }
}
