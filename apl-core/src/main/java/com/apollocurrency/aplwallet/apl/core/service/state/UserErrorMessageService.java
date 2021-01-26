package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.dex.core.model.UserErrorMessage;

import java.util.List;

public interface UserErrorMessageService {
    List<UserErrorMessage> getAllByAddress(String address, long toDbId, int limit);


    void add(UserErrorMessage errorMessage);

    List<UserErrorMessage> getAll(long toDbId, int limit);

    int deleteByTimestamp(long timestamp);
}
