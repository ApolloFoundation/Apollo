package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;

import java.util.List;

public interface UserErrorMessageService {
    List<UserErrorMessage> getAllByAddress( String address, long toDbId, int limit);


    void add(UserErrorMessage errorMessage);

    List<UserErrorMessage> getAll(long toDbId, int limit);

    int deleteByTimestamp(long timestamp);
}
