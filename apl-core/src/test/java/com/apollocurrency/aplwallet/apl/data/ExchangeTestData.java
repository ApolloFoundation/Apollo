/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;

public class ExchangeTestData {

    public final Exchange EXCHANGE_0 = createExchange(
        1,     1304688235223891922L,    -6000860677406393688L,   -2887846745475647200L,
        8732480699736433017L,    3494172333733565977L,    3494172333733565977L,    5000000000L,
        1,      45372444, 609481);
    public final Exchange EXCHANGE_1 = createExchange(
        2,     5294250207343561634L,    -6000860677406393688L,   -8152571865264379755L,
        8732480699736433017L,    3494172333733565977L,    3494172333733565977L,    100000000L,
        1,      45535320, 612120);
    public final Exchange EXCHANGE_2 = createExchange(
        3,     -9005611557904280410L,   9017193931881541951L,    3255354073311876604L,
        -5520700017789034517L,   -208393164898941117L,    -208393164898941117L,    100,
        1,      59450358, 1383308);
    public final Exchange EXCHANGE_3 = createExchange(
        4,     -6727424768559823653L,   9017193931881541951L,    3255354073311876604L,
        -5520700017789034517L,   -208393164898941117L,    9211698109297098287L,    200,
        1,      59450358, 1383308);
    public final Exchange EXCHANGE_4 = createExchange(
        5,     7546393628201945486L,    1829902366663355623L,    6734429651406997110L,
        3697010724017064611L,    7477442401604846627L,    7477442401604846627L,    300,
        1,      59450509, 1383324);
    public final Exchange EXCHANGE_5 = createExchange(
        6,     4698684103323222902L,    1829902366663355623L,    6734429651406997110L,
        3697010724017064611L,    7477442401604846627L,    9211698109297098287L,    400,
        1,      59450509, 1383324);

    public final Exchange EXCHANGE_NEW = createNewExchange(4698684103323222701L,    1829902366663355623L,    6734429651406997110L,
        3697010724017064611L,    7477442401604846627L,    9211698109297098287L,    500,
        1,      59450519, 1383334);

    public List<Exchange> ALL_EXCHANGE_ORDERED_BY_DBID = List.of(
        EXCHANGE_0, EXCHANGE_1, EXCHANGE_2, EXCHANGE_3, EXCHANGE_4, EXCHANGE_5
    );

    public Exchange createExchange(long dbId, long transactionId, long currencyId, long blockId, long offerId,
                                   long sellerId, long buyerId, long units, long rateATM, int timestamp, int height) {
        Exchange exchange = new Exchange(transactionId, currencyId, blockId, offerId,
            sellerId, buyerId, units, rateATM, timestamp, height);
        exchange.setDbId(dbId);
        return exchange;
    }

    public Exchange createNewExchange(long transactionId, long currencyId, long blockId, long offerId,
                                      long sellerId, long buyerId, long units, long rateATM, int timestamp, int height) {
        return new Exchange(transactionId, currencyId, blockId, offerId,
            sellerId, buyerId, units, rateATM, timestamp, height);
    }

}
