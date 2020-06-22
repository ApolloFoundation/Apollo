/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;

import java.util.List;


public class ExchangeRequestTestData {

    public final ExchangeRequest EXCHANGE_REQUEST_0 = createExchangeRequest(
        1,     1304688235223891922L,    3494172333733565977L,    -6000860677406393688L,
        5000000000L, 50,   true,   45372444,   609481);
    public final ExchangeRequest EXCHANGE_REQUEST_1 = createExchangeRequest(
        2,     5294250207343561634L,    3494172333733565977L,    -6000860677406393688L,
        100000000L,  1,    true,   45535320,   612120);
    public final ExchangeRequest EXCHANGE_REQUEST_2 = createExchangeRequest(
        3,     -4059997508574268268L,   3494172333733565977L,    -6000860677406393688L,
        2500000000L, 25,   false,  45535497,   612121);
    public final ExchangeRequest EXCHANGE_REQUEST_3 = createExchangeRequest(
        4,     -9005611557904280410L,   -208393164898941117L,    9017193931881541951L,
        1,        1,    false,  59450358,   1383308);
    public final ExchangeRequest EXCHANGE_REQUEST_4 = createExchangeRequest(
        5,     -6727424768559823653L,   9211698109297098287L,    9017193931881541951L,
        1,        1,    true,   59450358,   1383308);
    public final ExchangeRequest EXCHANGE_REQUEST_5 = createExchangeRequest(
        6,     7546393628201945486L,    7477442401604846627L,    1829902366663355623L,
        1,        1,    false,  59450509,   1383324);
    public final ExchangeRequest EXCHANGE_REQUEST_6 = createExchangeRequest(
        7,     4698684103323222902L,    9211698109297098287L,    1829902366663355623L,
        1,        1,    true,   59450509,   1383324);

    public final ExchangeRequest EXCHANGE_REQUEST_NEW = createNewExchangeRequest(
        6898684103323222902L,    9211698109297098287L,    1829902366663355623L,
        100,        1,    true,   59450509,   1383324);

    public List<ExchangeRequest> ALL_EXCHANGE_REQUEST_ORDERED_BY_DBID = List.of(
        EXCHANGE_REQUEST_6, EXCHANGE_REQUEST_5, EXCHANGE_REQUEST_4, EXCHANGE_REQUEST_3, EXCHANGE_REQUEST_2, EXCHANGE_REQUEST_1, EXCHANGE_REQUEST_0);

    public ExchangeRequest createExchangeRequest(long dbId, long id, long accountId, long currencyId, long units,
                                                 long rate, boolean isBuy, int timestamp, int height) {
        ExchangeRequest assetDelete = new ExchangeRequest(id, accountId, currencyId, units,
            rate, isBuy, height, timestamp);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public ExchangeRequest createNewExchangeRequest(long id, long accountId, long currencyId, long units,
                                                    long rate, boolean isBuy, int timestamp, int height) {
        return new ExchangeRequest(id, accountId, currencyId, units,
            rate, isBuy, height, timestamp);
    }


}
